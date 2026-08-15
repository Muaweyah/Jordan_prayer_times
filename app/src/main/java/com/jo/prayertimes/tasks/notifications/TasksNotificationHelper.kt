package com.jo.prayertimes.tasks.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object TasksNotificationHelper {
    const val CHANNEL_ID = "prayertimes_tasks_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات المهام",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "تنبيهات لمهامك اليومية" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, notificationId: Int, title: String) {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("تذكير: $title")
            .setContentText("حان وقت هذه المهمة")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) { }
    }
}
