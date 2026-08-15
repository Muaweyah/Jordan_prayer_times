package com.jo.prayertimes.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TasksAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "مهمة"
        val taskId = intent.getIntExtra("taskId", 0)
        TasksNotificationHelper.show(context, taskId, title)
    }
}
