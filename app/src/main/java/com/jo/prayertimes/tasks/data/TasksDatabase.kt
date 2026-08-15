package com.jo.prayertimes.tasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class, Category::class], version = 1, exportSchema = false)
abstract class TasksDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: TasksDatabase? = null

        fun getInstance(context: Context): TasksDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TasksDatabase::class.java,
                    "prayertimes_tasks_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
