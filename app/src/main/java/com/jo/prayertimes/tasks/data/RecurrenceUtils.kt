package com.jo.prayertimes.tasks.data

import java.util.Calendar

/** يحدد إذا كانت مهمة متكررة (يومية/أسبوعية/شهرية/سنوية) مفعّلة بتاريخ معيّن، بمعزل عن واجهة العرض. */
object RecurrenceUtils {
    fun isActiveOn(task: Task, cal: Calendar): Boolean {
        return when (task.recurrenceType) {
            "MONTHLY" -> task.monthDay != null && cal.get(Calendar.DAY_OF_MONTH) == task.monthDay
            "YEARLY" -> task.yearMonth != null && task.yearDay != null &&
                (cal.get(Calendar.MONTH) + 1) == task.yearMonth && cal.get(Calendar.DAY_OF_MONTH) == task.yearDay
            else -> {
                val weekday = cal.get(Calendar.DAY_OF_WEEK) - 1
                val days = task.recurrenceDays?.split(",")?.mapNotNull { it.toIntOrNull() } ?: (0..6).toList()
                days.contains(weekday)
            }
        }
    }
}
