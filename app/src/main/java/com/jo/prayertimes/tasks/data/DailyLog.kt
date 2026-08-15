package com.jo.prayertimes.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs", primaryKeys = ["taskId", "date"])
data class DailyLog(
    val taskId: Long,
    val date: String,
    val completed: Boolean = false,
    val punished: Boolean = false
)
