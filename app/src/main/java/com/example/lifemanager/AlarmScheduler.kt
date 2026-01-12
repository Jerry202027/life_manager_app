package com.example.lifemanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.lifemanager.data.Task
import java.util.Calendar

interface AlarmScheduler {
    fun schedule(task: Task)
    fun cancel(task: Task)
}

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: Task) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.scheduledDate
            add(Calendar.MINUTE, task.scheduledTimeMinutes)
        }

        // 如果時間已經過去，就不再設定鬧鐘
        if (calendar.timeInMillis < System.currentTimeMillis()) return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
        }
        
        // 使用 Task ID 作為 PendingIntent 的 Request Code，確保每個鬧鐘都是唯一的
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 檢查是否有精確鬧鐘權限
        if (alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // 在某些極端情況下，即使 canScheduleExactAlarms() 回傳 true，仍可能拋出 SecurityException
                e.printStackTrace()
            }
        }
    }

    override fun cancel(task: Task) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
}