package com.jo.prayertimes.tasks.ui.simple

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

class TodayViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())

    private val _items = MutableStateFlow<List<SelectedTask>>(emptyList())
    val items: StateFlow<List<SelectedTask>> = _items

    private val _completedIds = MutableStateFlow<Set<Long>>(emptySet())
    val completedIds: StateFlow<Set<Long>> = _completedIds

    init {
        viewModelScope.launch {
            while (true) {
                val list = db.selectedTaskDao().getAllOnce()
                _items.value = list.sortedBy { item -> DefaultCategories.list.indexOfFirst { c -> c.id == item.categoryId } }
                val completed = mutableSetOf<Long>()
                for (t in list) {
                    val log = db.dailyLogDao().get(t.id, today)
                    if (log?.completed == true) completed.add(t.id)
                }
                _completedIds.value = completed
                delay(1000)
            }
        }
    }

    fun toggle(item: SelectedTask) {
        viewModelScope.launch {
            val nowDone = !_completedIds.value.contains(item.id)
            db.dailyLogDao().upsert(DailyLog(taskId = item.id, date = today, completed = nowDone))
        }
    }
}
