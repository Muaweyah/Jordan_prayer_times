package com.jo.prayertimes.tasks.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)
    suspend fun addTask(task: Task) = taskDao.insert(task)
    suspend fun updateTask(task: Task) = taskDao.update(task)
    suspend fun deleteTask(task: Task) = taskDao.delete(task)
}
