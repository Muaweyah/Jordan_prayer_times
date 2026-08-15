package com.jo.prayertimes.tasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY priority DESC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTasksInRange(startDate: String, endDate: String): List<Task>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isCompleted = 1")
    suspend fun getCompletedCount(date: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date")
    suspend fun getTotalCount(date: String): Int
}
