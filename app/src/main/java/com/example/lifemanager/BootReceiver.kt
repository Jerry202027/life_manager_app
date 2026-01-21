package com.example.lifemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifemanager.data.AppDatabase
import com.example.lifemanager.data.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 開機廣播接收器
 * 在設備重啟後重新排程所有尚未執行的任務鬧鐘
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        // 使用 goAsync() 來延長廣播接收器的生命週期
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val taskDao = database.taskDao()
                val alarmScheduler = AlarmSchedulerImpl(context)
                
                // 獲取當前時間的日期部分
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // 獲取所有未來的計劃中任務並重新排程
                val pendingTasks = taskDao.getPendingTasksFromDate(todayStart)
                
                for (task in pendingTasks) {
                    // 只為尚未開始的任務重新設定鬧鐘
                    if (task.status == TaskStatus.PLANNED) {
                        alarmScheduler.schedule(task)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

