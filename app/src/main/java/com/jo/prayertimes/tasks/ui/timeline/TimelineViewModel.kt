package com.jo.prayertimes.tasks.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TimelineBlock(
    val task: Task,
    val startMinutes: Int,
    val endMinutes: Int
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TasksDatabase.getInstance(application).taskDao()
    private val categoryDao = TasksDatabase.getInstance(application).categoryDao()
    private val repository = TaskRepository(dao)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var currentDate: String = dateFormat.format(Date())
        private set

    val visibleCategories: StateFlow<List<Category>> = categoryDao.getVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { categoryDao.insertAll(DefaultCategories.list) }
    }

    fun tasksForDate() = repository.getTasksForDate(currentDate)
    fun inboxTasks() = repository.getInboxTasks()

    fun toMinutes(time: String?): Int? {
        if (time.isNullOrBlank()) return null
        val parts = time.split(":")
        if (parts.size != 2) return null
        return (parts[0].toIntOrNull() ?: return null) * 60 + (parts[1].toIntOrNull() ?: return null)
    }

    fun buildBlocks(tasks: List<Task>): List<TimelineBlock> {
        return tasks.mapNotNull { t ->
            val start = toMinutes(t.startTime) ?: return@mapNotNull null
            val end = toMinutes(t.endTime) ?: (start + 30)
            TimelineBlock(t, start, end)
        }.sortedBy { it.startMinutes }
    }

    fun addTimedTask(
        title: String,
        categoryId: String,
        startTime: String,
        endTime: String,
        notes: String?,
        linkUrl: String?,
        isRecurring: Boolean,
        recurrenceDays: String?
    ) {
        viewModelScope.launch {
            repository.addTask(
                Task(
                    title = title,
                    category = categoryId,
                    date = currentDate,
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes,
                    linkUrl = linkUrl,
                    isRecurring = isRecurring,
                    recurrenceDays = recurrenceDays
                )
            )
        }
    }

    fun addInboxTask(title: String, categoryId: String) {
        viewModelScope.launch {
            repository.addTask(Task(title = title, category = categoryId, date = null))
        }
    }

    fun scheduleFromInbox(task: Task, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(date = currentDate, startTime = startTime, endTime = endTime)
            )
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            repository.updateTask(task.copy(isCompleted = nowCompleted))
            if (nowCompleted && task.itemType == "TODO") {
                GamificationService.applyTodoComplete(getApplication(), task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun addSubtask(parentId: Long, title: String, categoryId: String) {
        viewModelScope.launch {
            repository.addTask(
                Task(title = title, category = categoryId, date = null, parentTaskId = parentId)
            )
        }
    }

    fun subtasksFor(parentId: Long) = dao.getSubtasks(parentId)
}
