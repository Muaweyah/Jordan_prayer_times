package com.jo.prayertimes.tasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedTaskDao {
    @Query("SELECT * FROM selected_tasks")
    fun getAll(): Flow<List<SelectedTask>>

    @Query("SELECT * FROM selected_tasks")
    suspend fun getAllOnce(): List<SelectedTask>

    @Query("SELECT * FROM selected_tasks WHERE title = :title AND categoryId = :categoryId LIMIT 1")
    suspend fun find(title: String, categoryId: String): SelectedTask?

    @Insert
    suspend fun insert(item: SelectedTask): Long

    @Delete
    suspend fun delete(item: SelectedTask)

    @Query("DELETE FROM selected_tasks")
    suspend fun deleteAll()
}
