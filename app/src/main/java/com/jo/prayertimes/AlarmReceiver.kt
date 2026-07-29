package com.jo.prayertimes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmScheduler.ACTION_PLAY_ADHAN) {
            val prayerKey = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_KEY)
            val serviceIntent = Intent(context, AdhanService::class.java).apply {
                action = AdhanService.ACTION_PLAY_ADHAN
                putExtra(AlarmScheduler.EXTRA_PRAYER_KEY, prayerKey)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        // في كل مرة يعمل فيها المستقبِل (بعد أذان أو بعد إعادة الإقلاع) نعيد جدولة جميع التنبيهات القادمة
        AlarmScheduler(context).rescheduleAll()
    }
}
