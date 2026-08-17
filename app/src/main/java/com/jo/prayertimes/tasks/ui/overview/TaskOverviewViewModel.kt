package com.jo.prayertimes.tasks.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class OverviewPeriod { DAY, WEEK, MONTH, YEAR }

class TaskOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())

    private val _period = MutableStateFlow(OverviewPeriod.DAY)
    val period: StateFlow<OverviewPeriod> = _period

    private val _items = MutableStateFlow<List<Task>>(emptyList())
    val items: StateFlow<List<Task>> = _items

    private val _completedIds = MutableStateFlow<Set<Long>>(emptySet())
    val completedIds: StateFlow<Set<Long>> = _completedIds

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(1500)
            }
        }
    }

    fun setPeriod(p: OverviewPeriod) {
        _period.value = p
    }

    private suspend fun refresh() {
        val all = db.taskDao().getRecurringTasks().filter { it.itemType == "DAILY" }
        val filtered = when (_period.value) {
            OverviewPeriod.DAY -> all.filter { RecurrenceUtils.isActiveOn(it, Calendar.getInstance()) }
            OverviewPeriod.WEEK -> all.filter { it.recurrenceType == "WEEKLY" }
            OverviewPeriod.MONTH -> all.filter { it.recurrenceType == "MONTHLY" }
            OverviewPeriod.YEAR -> all.filter { it.recurrenceType == "YEARLY" }
        }
        _items.value = filtered.sortedBy { DefaultCategories.list.indexOfFirst { c -> c.id == it.category } }

        val completed = mutableSetOf<Long>()
        for (t in filtered) {
            val log = db.dailyLogDao().get(t.id, today)
            if (log?.completed == true) completed.add(t.id)
        }
        _completedIds.value = completed
    }

    fun toggle(task: Task) {
        viewModelScope.launch {
            val nowDone = !_completedIds.value.contains(task.id)
            GamificationService.applyDailyToggle(getApplication(), task, nowDone)
            refresh()
        }
    }
}
