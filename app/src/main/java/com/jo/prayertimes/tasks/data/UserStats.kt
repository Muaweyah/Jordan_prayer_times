package com.jo.prayertimes.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val health: Int = 50,
    val maxHealth: Int = 50,
    val xp: Int = 0,
    val level: Int = 1,
    val gold: Int = 0,
    val lastRolloverDate: String? = null,
    val defaultDailiesSeeded: Boolean = false
)
