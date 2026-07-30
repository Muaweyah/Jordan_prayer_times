package com.jo.prayertimes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AdhanService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var flipDetector: FlipDetector? = null
    private var volumeButtonReceiver: BroadcastReceiver? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ADHAN) {
            stopAdhan()
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerKey = intent?.getStringExtra(AlarmScheduler.EXTRA_PRAYER_KEY)
        startForeground(NOTIFICATION_ID, buildNotification(prayerKey))
        playAdhan()
        return START_STICKY
    }

    private fun buildNotification(prayerKey: String?): android.app.Notification {
        val prayer = Prayer.values().find { it.key == prayerKey }
        val title = if (prayer != null) "حان الآن موعد أذان ${prayer.arabicLabel}" else "حان الآن موعد الأذان"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "تنبيهات الأذان", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AdhanService::class.java).apply { action = ACTION_STOP_ADHAN }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("اضغط لإيقاف الأذان")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "إيقاف", stopPendingIntent)
            .build()
    }

    private fun playAdhan() {
        stopPlaybackOnly()
        try {
            isPlaying = true
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = resources.openRawResourceFd(R.raw.adhan)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
                setOnCompletionListener {
                    stopAdhan()
                    stopSelf()
                }
            }

            val prefs = getSharedPreferences("PrayerAppSettings", Context.MODE_PRIVATE)
            val isFlipEnabled = prefs.getBoolean("flip_to_mute", true)

            if (isFlipEnabled) {
                flipDetector = FlipDetector(this) {
                    stopAdhan()
                    Toast.makeText(applicationContext, "تم كتم التنبيه لقلب الهاتف", Toast.LENGTH_SHORT).show()
                    stopSelf()
                }
                flipDetector?.start()
            }

            val isVolumeButtonMuteEnabled = prefs.getBoolean("volume_button_mute", true)
            if (isVolumeButtonMuteEnabled) {
                registerVolumeButtonReceiver()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    /** يراقب أي تغيير في مستوى الصوت (بما فيه الضغط على أزرار الصوت الفعلية) ويكتم الأذان فور حدوثه */
    private fun registerVolumeButtonReceiver() {
        if (volumeButtonReceiver != null) return
        volumeButtonReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopAdhan()
                Toast.makeText(applicationContext, "تم كتم التنبيه بالضغط على زر الصوت", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        ContextCompat.registerReceiver(this, volumeButtonReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterVolumeButtonReceiver() {
        volumeButtonReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // كان غير مسجل أصلًا، لا داعي لأي إجراء
            }
        }
        volumeButtonReceiver = null
    }

    private fun stopPlaybackOnly() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun stopAdhan() {
        isPlaying = false
        flipDetector?.stop()
        flipDetector = null
        unregisterVolumeButtonReceiver()
        stopPlaybackOnly()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdhan()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PLAY_ADHAN = "com.jo.prayertimes.ACTION_PLAY_ADHAN"
        const val ACTION_STOP_ADHAN = "com.jo.prayertimes.ACTION_STOP_ADHAN"
        private const val CHANNEL_ID = "adhan_channel"
        private const val NOTIFICATION_ID = 501

        var isPlaying: Boolean = false
            private set
    }
}
