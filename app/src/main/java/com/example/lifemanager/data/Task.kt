package com.example.lifemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 定義任務狀態
enum class TaskStatus {
    PLANNED,    // 計劃中
    IN_PROGRESS, // 進行中 (鎖定中)
    COMPLETED,   // 已完成 (已填寫紀錄)
    ABANDONED    // 放棄
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val title: String,                // 任務名稱
    val description: String = "",     // 詳細說明
    
    // 排程相關
    val scheduledDate: Long,          // 預定日期 (存當天 00:00 的 Timestamp)
    val scheduledTimeMinutes: Int,    // 預定當天第幾分鐘開始 (例如 10:30 = 630)
    val plannedDurationMinutes: Long, // 預計時長
    
    val color: Int = -16776961,       // 顯示顏色 (ARGB Int, 預設藍色)
    
    // 執行紀錄
    val startTime: Long? = null,      // 實際開始時間 (Timestamp)
    val endTime: Long? = null,        // 實際結束時間
    
    val status: TaskStatus = TaskStatus.PLANNED,
    
    val workLog: String? = null,      // 心得
    
    val createdAt: Long = System.currentTimeMillis()
)