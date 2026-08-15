package com.jo.prayertimes.tasks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import com.jo.prayertimes.tasks.notifications.TasksReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val categoryDao = TasksDatabase.getInstance(application).categoryDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val appContext = application.applicationContext

    var currentDate: String = dateFormat.format(Date())
        private set

    val visibleCategories: StateFlow<List<Category>> = categoryDao.getVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val dao = TasksDatabase.getInstance(application).taskDao()
        repository = TaskRepository(dao)
        viewModelScope.launch { categoryDao.insertAll(DefaultCategories.list) }
    }

    fun tasksForCurrentDate(): Flow<List<Task>> = repository.getTasksForDate(currentDate)

    fun addTask(title: String, categoryId: String, reminderTime: String?) {
        viewModelScope.launch {
            val newId = repository.addTask(
                Task(title = title, category = categoryId, date = currentDate, reminderTime = reminderTime)
            )
            if (reminderTime != null) {
                TasksReminderScheduler.schedule(appContext, newId, title, currentDate, reminderTime)
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task.copy(isCompleted = !task.isCompleted)) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            TasksReminderScheduler.cancel(appContext, task.id)
        }
    }
}
