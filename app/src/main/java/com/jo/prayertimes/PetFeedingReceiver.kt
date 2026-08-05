package com.jo.prayertimes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class PetFeedingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PetFeedingScheduler.ACTION_FEED_PET) {
            val mealId = intent.getIntExtra(PetFeedingScheduler.EXTRA_MEAL_ID, -1)
            val manager = PetFeedingManager(context)
            val pet = manager.getPets().find { p -> p.meals.any { it.id == mealId } }
            if (pet != null) {
                showFeedingNotification(context, pet)
            }
        }
        // إعادة جدولة كل التنبيهات القادمة (بما فيها موعد الغد لهذه الوجبة نفسها)
        PetFeedingScheduler(context).rescheduleAll()
    }

    private fun showFeedingNotification(context: Context, pet: Pet) {
        val channelId = "pet_feeding_channel_${pet.id}"
        val soundUri: Uri = if (pet.soundUri != null) {
            Uri.parse(pet.soundUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // نغمة القناة لا يمكن تغييرها بعد إنشائها؛ نحذفها ونعيد إنشاءها بالنغمة الحالية
            // المختارة لهذا الحيوان في كل مرة، حتى تُطبَّق أي نغمة جديدة يختارها المستخدم لاحقاً
            manager?.deleteNotificationChannel(channelId)
            val channel = NotificationChannel(
                channelId, "تذكير إطعام ${pet.name}", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
            manager?.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, PetFeedingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, pet.id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("حان وقت إطعام ${pet.name} 🐾")
            .setContentText("لا تنسَ وجبة ${pet.name} الآن")
            .setSmallIcon(R.drawable.ic_paw)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(contentPendingIntent)
            .build()

        manager?.notify(30000 + pet.id, notification)
    }
}
