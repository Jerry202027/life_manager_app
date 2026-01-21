package com.example.lifemanager.core.data.repository

import com.example.lifemanager.core.data.local.TaskDao
import com.example.lifemanager.core.data.local.entity.TaskEntity
import com.example.lifemanager.core.data.local.entity.toDomain
import com.example.lifemanager.core.data.local.entity.toEntity
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.core.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    
    override fun getTasksForDate(date: Long): Flow<List<Task>> {
        return taskDao.getTasksByDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }
    
    override suspend fun getPendingTasksFromDate(fromDate: Long): List<Task> {
        return taskDao.getPendingTasksFromDate(fromDate).map { it.toDomain() }
    }
    
    override suspend fun createTask(
        title: String,
        durationMinutes: Long,
        scheduledDate: Long,
        scheduledTimeMinutes: Int,
        color: Int
    ): Task {
        val entity = TaskEntity(
            title = title,
            plannedDurationMinutes = durationMinutes,
            scheduledDate = scheduledDate,
            scheduledTimeMinutes = scheduledTimeMinutes,
            color = color,
            status = TaskStatus.PLANNED
        )
        val newId = taskDao.insertTask(entity)
        return taskDao.getTaskById(newId.toInt())!!.toDomain()
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }
    
    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
    
    override suspend fun startTask(task: Task): Task {
        val updatedTask = task.copy(
            status = TaskStatus.IN_PROGRESS,
            startTime = System.currentTimeMillis()
        )
        taskDao.updateTask(updatedTask.toEntity())
        return updatedTask
    }
    
    override suspend fun completeTask(task: Task, workLog: String): Task {
        val updatedTask = task.copy(
            status = TaskStatus.COMPLETED,
            endTime = System.currentTimeMillis(),
            workLog = workLog
        )
        taskDao.updateTask(updatedTask.toEntity())
        return updatedTask
    }
}

