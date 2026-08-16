package com.jo.prayertimes.tasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date AND parentTaskId IS NULL ORDER BY startTime IS NULL, startTime ASC, priority DESC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date IS NULL AND parentTaskId IS NULL AND itemType = 'TODO' ORDER BY id DESC")
    fun getInboxTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY id ASC")
    fun getSubtasks(parentId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate AND parentTaskId IS NULL")
    suspend fun getTasksInRange(startDate: String, endDate: String): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE title = :title AND date = :date AND category = :category")
    suspend fun countMatching(title: String, date: String, category: String): Int

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET date = :newDate WHERE id = :taskId")
    suspend fun moveToDate(taskId: Long, newDate: String)

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isCompleted = 1 AND parentTaskId IS NULL")
    suspend fun getCompletedCount(date: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND parentTaskId IS NULL")
    suspend fun getTotalCount(date: String): Int
}
