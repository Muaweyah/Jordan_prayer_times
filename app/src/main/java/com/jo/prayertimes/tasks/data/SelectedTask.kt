package com.jo.prayertimes.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "selected_tasks")
data class SelectedTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val categoryId: String,
    val isCustom: Boolean = false,
    val reminderTime: String? = null,
    val reminderEnabled: Boolean = false
)
