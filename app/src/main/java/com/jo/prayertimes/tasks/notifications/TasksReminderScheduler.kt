package com.jo.prayertimes.tasks.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*

object TasksReminderScheduler {
    fun schedule(context: Context, taskId: Long, title: String, date: String, time: String) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val triggerTime = format.parse("$date $time")?.time ?: return
        val intent = Intent(context, TasksAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("taskId", taskId.toInt())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) { }
    }

    fun cancel(context: Context, taskId: Long) {
        val intent = Intent(context, TasksAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
