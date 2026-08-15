package com.jo.prayertimes.tasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM task_categories ORDER BY isHidden ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM task_categories WHERE isHidden = 0")
    fun getVisible(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Insert
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
