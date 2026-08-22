package com.jo.prayertimes.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SimpleReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", 0)
        val title = intent.getStringExtra("title") ?: return
        val time = intent.getStringExtra("time") ?: return

        TasksNotificationHelper.show(context, taskId.toInt(), title)
        SimpleReminderScheduler.scheduleNextDay(context, taskId, title, time)
    }
}
