package com.jo.prayertimes.tasks.ui.gamify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val stats: StateFlow<UserStats> = db.userStatsDao().observe()
        .map { it ?: UserStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    val categories: StateFlow<List<Category>> = db.categoryDao().getVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<Task>> = db.taskDao().getInboxTasks()
        .map { emptyList<Task>() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allHabits = kotlinx.coroutines.flow.MutableStateFlow<List<Task>>(emptyList())
    val habitsList: StateFlow<List<Task>> = _allHabits

    init {
        viewModelScope.launch {
            _allHabits.value = db.taskDao().getRecurringTasks().filter { it.itemType == "HABIT" }
        }
        viewModelScope.launch {
            while (true) {
                _allHabits.value = db.taskDao().getRecurringTasks().filter { it.itemType == "HABIT" }
                kotlinx.coroutines.delay(1500)
            }
        }
    }

    fun addHabit(title: String, categoryId: String, difficulty: String, positive: Boolean, negative: Boolean) {
        viewModelScope.launch {
            db.taskDao().insert(
                Task(
                    title = title,
                    category = categoryId,
                    date = null,
                    itemType = "HABIT",
                    difficulty = difficulty,
                    isPositiveHabit = positive,
                    isNegativeHabit = negative,
                    createdDate = dateFormat.format(Date())
                )
            )
        }
    }

    fun tapPositive(task: Task) {
        viewModelScope.launch { GamificationService.applyHabit(getApplication(), task, true) }
    }

    fun tapNegative(task: Task) {
        viewModelScope.launch { GamificationService.applyHabit(getApplication(), task, false) }
    }

    fun deleteHabit(task: Task) {
        viewModelScope.launch { db.taskDao().delete(task) }
    }
}
