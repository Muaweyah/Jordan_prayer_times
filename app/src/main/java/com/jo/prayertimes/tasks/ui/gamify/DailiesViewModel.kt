package com.jo.prayertimes.tasks.ui.gamify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailiesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())
    private val todayCal = Calendar.getInstance()

    val stats: StateFlow<UserStats> = db.userStatsDao().observe()
        .map { it ?: UserStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    val categories: StateFlow<List<Category>> = db.categoryDao().getVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailies = MutableStateFlow<List<Task>>(emptyList())
    val dailies: StateFlow<List<Task>> = _dailies

    private val _completedToday = MutableStateFlow<Set<Long>>(emptySet())
    val completedToday: StateFlow<Set<Long>> = _completedToday

    init {
        viewModelScope.launch {
            GamificationService.resetSeedFlagOnce(getApplication())
            GamificationService.seedDefaultDailiesIfNeeded(getApplication())
        }
        viewModelScope.launch {
            while (true) {
                val all = db.taskDao().getRecurringTasks()
                _dailies.value = all.filter { t ->
                    t.itemType == "DAILY" && RecurrenceUtils.isActiveOn(t, todayCal)
                }
                val completed = mutableSetOf<Long>()
                for (t in _dailies.value) {
                    val log = db.dailyLogDao().get(t.id, today)
                    if (log?.completed == true) completed.add(t.id)
                }
                _completedToday.value = completed
                delay(1500)
            }
        }
    }

    fun addDaily(
        title: String,
        categoryId: String,
        difficulty: String,
        recurrenceType: String,
        recurrenceDays: List<Int>?,
        monthDay: Int?,
        yearMonth: Int?,
        yearDay: Int?
    ) {
        viewModelScope.launch {
            db.taskDao().insert(
                Task(
                    title = title,
                    category = categoryId,
                    date = null,
                    itemType = "DAILY",
                    difficulty = difficulty,
                    isRecurring = true,
                    recurrenceType = recurrenceType,
                    recurrenceDays = recurrenceDays?.joinToString(","),
                    monthDay = monthDay,
                    yearMonth = yearMonth,
                    yearDay = yearDay,
                    createdDate = dateFormat.format(Date())
                )
            )
        }
    }

    fun toggle(task: Task) {
        viewModelScope.launch {
            val alreadyDone = _completedToday.value.contains(task.id)
            GamificationService.applyDailyToggle(getApplication(), task, !alreadyDone)
        }
    }

    fun deleteDaily(task: Task) {
        viewModelScope.launch { db.taskDao().delete(task) }
    }
}
