package com.jo.prayertimes.tasks.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)
    fun getInboxTasks(): Flow<List<Task>> = taskDao.getInboxTasks()
    fun getSubtasks(parentId: Long): Flow<List<Task>> = taskDao.getSubtasks(parentId)
    suspend fun addTask(task: Task): Long = taskDao.insert(task)
    suspend fun updateTask(task: Task) = taskDao.update(task)
    suspend fun deleteTask(task: Task) = taskDao.delete(task)
    suspend fun moveToDate(taskId: Long, newDate: String) = taskDao.moveToDate(taskId, newDate)
}
