package com.jo.prayertimes.tasks.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate

    private val _items = MutableStateFlow<List<SelectedTask>>(emptyList())
    val items: StateFlow<List<SelectedTask>> = _items

    private val _completedIds = MutableStateFlow<Set<Long>>(emptySet())
    val completedIds: StateFlow<Set<Long>> = _completedIds

    init { load() }

    fun goToPreviousDay() {
        val cal = _selectedDate.value.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = cal
        load()
    }

    fun goToNextDay() {
        val cal = _selectedDate.value.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 1)
        if (!cal.after(Calendar.getInstance())) {
            _selectedDate.value = cal
            load()
        }
    }

    fun setDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        if (!cal.after(Calendar.getInstance())) {
            _selectedDate.value = cal
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val dateStr = dateFormat.format(_selectedDate.value.time)
            val selected = db.selectedTaskDao().getAllOnce()
            _items.value = selected.sortedBy { DefaultCategories.list.indexOfFirst { c -> c.id == it.categoryId } }
            val logs = db.dailyLogDao().getForDate(dateStr)
            _completedIds.value = logs.filter { it.completed }.map { it.taskId }.toSet()
        }
    }
}
