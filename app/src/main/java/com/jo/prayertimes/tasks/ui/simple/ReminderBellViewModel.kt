package com.jo.prayertimes.tasks.ui.simple

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.SelectedTask
import com.jo.prayertimes.tasks.data.TasksDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ReminderBellViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)

    val reminders: StateFlow<List<SelectedTask>> = db.selectedTaskDao().getAllWithReminders()
        .map { list -> list.sortedBy { it.reminderTime } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
