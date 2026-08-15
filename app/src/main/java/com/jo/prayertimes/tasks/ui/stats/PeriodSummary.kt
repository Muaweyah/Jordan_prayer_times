package com.jo.prayertimes.tasks.ui.stats

data class PeriodSummary(
    val label: String,
    val total: Int,
    val completed: Int
) {
    val ratio: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}

data class CategorySummary(
    val categoryId: String,
    val icon: String,
    val nameAr: String,
    val total: Int,
    val completed: Int
) {
    val ratio: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}
