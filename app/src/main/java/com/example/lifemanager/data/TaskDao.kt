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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
}