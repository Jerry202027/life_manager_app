package com.example.lifemanager.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.core.domain.model.TaskStatus

/**
 * Room entity for Task
 * This is the database representation
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val scheduledDate: Long,
    val scheduledTimeMinutes: Int,
    val plannedDurationMinutes: Long,
    val color: Int = DEFAULT_COLOR,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val status: TaskStatus = TaskStatus.PLANNED,
    val workLog: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_COLOR = -16776961 // Blue (ARGB)
    }
}

/**
 * Extension function to convert Entity to Domain model
 */
fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    scheduledDate = scheduledDate,
    scheduledTimeMinutes = scheduledTimeMinutes,
    plannedDurationMinutes = plannedDurationMinutes,
    color = color,
    startTime = startTime,
    endTime = endTime,
    status = status,
    workLog = workLog,
    createdAt = createdAt
)

/**
 * Extension function to convert Domain model to Entity
 */
fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    scheduledDate = scheduledDate,
    scheduledTimeMinutes = scheduledTimeMinutes,
    plannedDurationMinutes = plannedDurationMinutes,
    color = color,
    startTime = startTime,
    endTime = endTime,
    status = status,
    workLog = workLog,
    createdAt = createdAt
)

