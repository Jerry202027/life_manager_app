package com.example.lifemanager.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao) {
    
    fun getTasksForDate(date: Long): Flow<List<Task>> = taskDao.getTasksByDate(date)

    // MODIFIED: Make this function return the created Task with its auto-generated ID
    suspend fun addTask(
        title: String, 
        durationMinutes: Long, 
        scheduledDate: Long,
        scheduledTimeMinutes: Int,
        color: Int
    ): Task {
        val newTask = Task(
            title = title,
            plannedDurationMinutes = durationMinutes,
            scheduledDate = scheduledDate,
            scheduledTimeMinutes = scheduledTimeMinutes,
            color = color,
            status = TaskStatus.PLANNED
        )
        val newId = taskDao.insertTask(newTask)
        // After inserting, get the task back with the generated ID
        return taskDao.getTaskById(newId.toInt())!!
    }

    suspend fun getTask(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
}