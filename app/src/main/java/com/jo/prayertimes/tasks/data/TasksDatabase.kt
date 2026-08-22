package com.jo.prayertimes.tasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, Category::class, UserStats::class, DailyLog::class, SelectedTask::class],
    version = 6,
    exportSchema = false
)
abstract class TasksDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun selectedTaskDao(): SelectedTaskDao

    companion object {
        @Volatile private var INSTANCE: TasksDatabase? = null

        fun getInstance(context: Context): TasksDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TasksDatabase::class.java,
                    "prayertimes_tasks_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
