package com.jo.prayertimes.tasks.ui.simple

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import com.jo.prayertimes.tasks.notifications.SimpleReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)

    val selected: StateFlow<List<SelectedTask>> = db.selectedTaskDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTemplate(title: String, categoryId: String) {
        viewModelScope.launch {
            val existing = db.selectedTaskDao().find(title, categoryId)
            if (existing != null) {
                SimpleReminderScheduler.cancel(getApplication(), existing.id)
                db.selectedTaskDao().delete(existing)
            } else {
                db.selectedTaskDao().insert(SelectedTask(title = title, categoryId = categoryId))
            }
        }
    }

    fun addCustom(title: String, categoryId: String) {
        viewModelScope.launch {
            db.selectedTaskDao().insert(SelectedTask(title = title, categoryId = categoryId, isCustom = true))
        }
    }

    fun removeCustom(item: SelectedTask) {
        viewModelScope.launch {
            SimpleReminderScheduler.cancel(getApplication(), item.id)
            db.selectedTaskDao().delete(item)
        }
    }

    fun setReminderTime(item: SelectedTask, time: String) {
        viewModelScope.launch {
            val updated = item.copy(reminderTime = time, reminderEnabled = true)
            db.selectedTaskDao().update(updated)
            SimpleReminderScheduler.schedule(getApplication(), updated.id, updated.title, time)
        }
    }

    fun toggleReminder(item: SelectedTask, enabled: Boolean) {
        viewModelScope.launch {
            val updated = item.copy(reminderEnabled = enabled)
            db.selectedTaskDao().update(updated)
            val time = updated.reminderTime
            if (enabled && time != null) {
                SimpleReminderScheduler.schedule(getApplication(), updated.id, updated.title, time)
            } else {
                SimpleReminderScheduler.cancel(getApplication(), updated.id)
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val all = db.selectedTaskDao().getAllOnce()
            for (item in all) {
                SimpleReminderScheduler.cancel(getApplication(), item.id)
            }
            db.selectedTaskDao().deleteAll()
            db.dailyLogDao().deleteAll()
        }
    }
}
