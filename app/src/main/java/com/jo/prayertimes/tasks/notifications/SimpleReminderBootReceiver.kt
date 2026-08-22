package com.jo.prayertimes.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jo.prayertimes.tasks.data.TasksDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimpleReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val items = TasksDatabase.getInstance(appContext).selectedTaskDao().getAllWithRemindersOnce()
            for (item in items) {
                val time = item.reminderTime ?: continue
                SimpleReminderScheduler.schedule(appContext, item.id, item.title, time)
            }
        }
    }
}
