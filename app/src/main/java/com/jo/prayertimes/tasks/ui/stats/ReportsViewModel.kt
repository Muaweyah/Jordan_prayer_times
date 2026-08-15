package com.jo.prayertimes.tasks.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.TasksDatabase
import com.jo.prayertimes.tasks.data.DefaultCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReportPeriod { DAY, WEEK, MONTH, YEAR }

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _period = MutableStateFlow(ReportPeriod.DAY)
    val period: StateFlow<ReportPeriod> = _period

    private val _overall = MutableStateFlow(PeriodSummary("", 0, 0))
    val overall: StateFlow<PeriodSummary> = _overall

    private val _byCategory = MutableStateFlow<List<CategorySummary>>(emptyList())
    val byCategory: StateFlow<List<CategorySummary>> = _byCategory

    private val _dailyBreakdown = MutableStateFlow<List<PeriodSummary>>(emptyList())
    val dailyBreakdown: StateFlow<List<PeriodSummary>> = _dailyBreakdown

    init { load() }

    fun setPeriod(p: ReportPeriod) {
        _period.value = p
        load()
    }

    private fun currentLocale(): Locale = Locale.getDefault()

    private fun load() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val start = cal.clone() as Calendar
            val end = cal.clone() as Calendar

            when (_period.value) {
                ReportPeriod.DAY -> { }
                ReportPeriod.WEEK -> {
                    start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                    end.time = start.time
                    end.add(Calendar.DAY_OF_YEAR, 6)
                }
                ReportPeriod.MONTH -> {
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                ReportPeriod.YEAR -> {
                    start.set(Calendar.DAY_OF_YEAR, 1)
                    end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
                }
            }

            val startStr = dateFormat.format(start.time)
            val endStr = dateFormat.format(end.time)
            val tasks = db.taskDao().getTasksInRange(startStr, endStr)

            _overall.value = PeriodSummary(
                label = periodLabel(),
                total = tasks.size,
                completed = tasks.count { it.isCompleted }
            )

            val grouped = tasks.groupBy { it.category }
            _byCategory.value = DefaultCategories.list.mapNotNull { cat ->
                val list = grouped[cat.id] ?: return@mapNotNull null
                CategorySummary(
                    categoryId = cat.id,
                    icon = cat.icon,
                    nameAr = cat.nameAr,
                    nameEn = cat.nameEn,
                    total = list.size,
                    completed = list.count { it.isCompleted }
                )
            }.sortedByDescending { it.total }

            if (_period.value != ReportPeriod.DAY) {
                val byDate = tasks.groupBy { it.date }
                val breakdown = mutableListOf<PeriodSummary>()
                val iter = start.clone() as Calendar
                while (!iter.after(end)) {
                    val d = dateFormat.format(iter.time)
                    val dayTasks = byDate[d] ?: emptyList()
                    breakdown.add(
                        PeriodSummary(
                            label = SimpleDateFormat("d/M", currentLocale()).format(iter.time),
                            total = dayTasks.size,
                            completed = dayTasks.count { it.isCompleted }
                        )
                    )
                    iter.add(Calendar.DAY_OF_YEAR, 1)
                }
                _dailyBreakdown.value = breakdown
            } else {
                _dailyBreakdown.value = emptyList()
            }
        }
    }

    private fun periodLabel(): String {
        val cal = Calendar.getInstance()
        val locale = currentLocale()
        val app = getApplication<Application>()
        return when (_period.value) {
            ReportPeriod.DAY -> SimpleDateFormat("EEEE، d MMMM", locale).format(cal.time)
            ReportPeriod.WEEK -> app.getString(com.jo.prayertimes.R.string.reports_this_week)
            ReportPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
            ReportPeriod.YEAR -> SimpleDateFormat("yyyy", locale).format(cal.time)
        }
    }
}
