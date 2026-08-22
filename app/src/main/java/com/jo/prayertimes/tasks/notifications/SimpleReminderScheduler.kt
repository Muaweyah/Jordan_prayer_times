package com.jo.prayertimes.tasks.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*

/** يجدول تذكيراً يومياً متكرراً لمهمة معيّنة بوقت محدد. عند إطلاق التنبيه يعيد جدولة نفسه لليوم التالي تلقائياً. */
object SimpleReminderScheduler {
    private fun buildPendingIntent(context: Context, taskId: Long, title: String, time: String): PendingIntent {
        val intent = Intent(context, SimpleReminderAlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("title", title)
            putExtra("time", time)
        }
        return PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** يجدول لأقرب وقت قادم (اليوم إذا لسا ما فات، وإلا غداً) */
    fun schedule(context: Context, taskId: Long, title: String, time: String) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        setAlarm(context, taskId, title, time, cal.timeInMillis)
    }

    /** تُستدعى من المستقبل نفسه بعد إطلاق التنبيه لإعادة الجدولة لليوم التالي مباشرة */
    fun scheduleNextDay(context: Context, taskId: Long, title: String, time: String) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 1)

        setAlarm(context, taskId, title, time, cal.timeInMillis)
    }

    private fun setAlarm(context: Context, taskId: Long, title: String, time: String, triggerMillis: Long) {
        val pendingIntent = buildPendingIntent(context, taskId, title, time)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (e: SecurityException) { }
    }

    fun cancel(context: Context, taskId: Long) {
        val intent = Intent(context, SimpleReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
