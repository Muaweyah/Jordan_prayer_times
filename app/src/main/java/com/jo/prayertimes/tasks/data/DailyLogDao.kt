package com.jo.prayertimes.tasks.data

import androidx.room.*

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE taskId = :taskId AND date = :date")
    suspend fun get(taskId: Long, date: String): DailyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyLog)

    @Query("SELECT * FROM daily_logs WHERE taskId = :taskId ORDER BY date DESC LIMIT 60")
    suspend fun recentForTask(taskId: Long): List<DailyLog>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :start AND :end")
    suspend fun getLogsInRange(start: String, end: String): List<DailyLog>
}
