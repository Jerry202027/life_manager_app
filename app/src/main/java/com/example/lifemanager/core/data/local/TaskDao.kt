package com.example.lifemanager.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifemanager.core.data.local.entity.TaskEntity
import com.example.lifemanager.core.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    /**
     * Get all tasks for a specific date, ordered by scheduled time
     */
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY scheduledTimeMinutes ASC")
    fun getTasksByDate(date: Long): Flow<List<TaskEntity>>

    /**
     * Get all tasks ordered by creation time (newest first)
     */
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * Get a single task by ID
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?
    
    /**
     * Get all pending tasks from a specific date (for rescheduling alarms)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE scheduledDate >= :fromDate AND status = :status 
        ORDER BY scheduledDate ASC, scheduledTimeMinutes ASC
    """)
    suspend fun getPendingTasksFromDate(
        fromDate: Long,
        status: TaskStatus = TaskStatus.PLANNED
    ): List<TaskEntity>

    /**
     * Insert a new task and return its ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    /**
     * Update an existing task
     */
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    /**
     * Delete a task
     */
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    /**
     * Delete a task by ID
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)
}

