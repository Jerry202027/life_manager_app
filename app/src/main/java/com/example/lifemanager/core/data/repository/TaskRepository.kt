package com.example.lifemanager.core.data.repository

import com.example.lifemanager.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Task operations
 * Defines the contract for data access, independent of implementation
 */
interface TaskRepository {
    
    /**
     * Get all tasks for a specific date as a Flow
     */
    fun getTasksForDate(date: Long): Flow<List<Task>>
    
    /**
     * Get all tasks as a Flow
     */
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * Get a single task by ID
     */
    suspend fun getTaskById(id: Int): Task?
    
    /**
     * Get all pending tasks from a specific date
     */
    suspend fun getPendingTasksFromDate(fromDate: Long): List<Task>
    
    /**
     * Create a new task and return it with the generated ID
     */
    suspend fun createTask(
        title: String,
        durationMinutes: Long,
        scheduledDate: Long,
        scheduledTimeMinutes: Int,
        color: Int
    ): Task
    
    /**
     * Update an existing task
     */
    suspend fun updateTask(task: Task)
    
    /**
     * Delete a task
     */
    suspend fun deleteTask(task: Task)
    
    /**
     * Start a task (change status to IN_PROGRESS)
     */
    suspend fun startTask(task: Task): Task
    
    /**
     * Complete a task with work log
     */
    suspend fun completeTask(task: Task, workLog: String): Task
}

