package com.jo.prayertimes.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val priority: Int = 0,
    val date: String,
    val isCompleted: Boolean = false,
    val reminderTime: String? = null,
    val repeatRule: String? = null,
    val customSound: String? = null
)
