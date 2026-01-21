package com.example.lifemanager.core.domain.model

/**
 * Domain model for Task
 * This is the business logic representation, independent of database or UI
 */
data class Task(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val scheduledDate: Long,
    val scheduledTimeMinutes: Int,
    val plannedDurationMinutes: Long,
    val color: Int,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val status: TaskStatus = TaskStatus.PLANNED,
    val workLog: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate the scheduled start time in milliseconds
     */
    val scheduledStartTimeMillis: Long
        get() = scheduledDate + (scheduledTimeMinutes * 60 * 1000L)
    
    /**
     * Check if the current time is past the scheduled start time
     */
    fun isPastScheduledTime(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMillis >= scheduledStartTimeMillis
    }
    
    /**
     * Get formatted time range string (e.g., "10:30 - 11:30")
     */
    fun getTimeRangeString(): String {
        val startHour = scheduledTimeMinutes / 60
        val startMin = scheduledTimeMinutes % 60
        val endTotalMinutes = scheduledTimeMinutes + plannedDurationMinutes.toInt()
        val endHour = (endTotalMinutes / 60) % 24
        val endMin = endTotalMinutes % 60
        return "%02d:%02d - %02d:%02d".format(startHour, startMin, endHour, endMin)
    }
    
    /**
     * Calculate actual duration in minutes (if task is completed)
     */
    fun getActualDurationMinutes(): Long? {
        return if (startTime != null && endTime != null) {
            (endTime - startTime) / 1000 / 60
        } else null
    }
}

/**
 * Task status enum
 */
enum class TaskStatus {
    PLANNED,      // 計劃中
    IN_PROGRESS,  // 進行中 (鎖定中)
    COMPLETED,    // 已完成 (已填寫紀錄)
    ABANDONED     // 放棄
}

