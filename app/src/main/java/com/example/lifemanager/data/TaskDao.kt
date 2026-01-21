package com.example.lifemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // 獲取某一天的所有任務
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY scheduledTimeMinutes ASC")
    fun getTasksByDate(date: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?
    
    // 獲取從指定日期開始的所有計劃中任務（用於開機後重新排程）
    @Query("SELECT * FROM tasks WHERE scheduledDate >= :fromDate AND status = 'PLANNED' ORDER BY scheduledDate ASC, scheduledTimeMinutes ASC")
    suspend fun getPendingTasksFromDate(fromDate: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
}