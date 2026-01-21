package com.example.lifemanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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

        val triggerTime = calendar.timeInMillis
        
        // 如果時間已經過去，就不再設定鬧鐘
        if (triggerTime < System.currentTimeMillis()) {
            Log.d("AlarmScheduler", "Task ${task.id} time has passed, not scheduling")
            return
        }

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
        
        try {
            // 使用 setAlarmClock - 這是最可靠的方式
            // 系統會把它當作真正的鬧鐘，不受 Doze 模式和背景限制影響
            // 並且會在狀態列顯示鬧鐘圖示
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
                putExtra(TaskAlarmReceiver.ACTION_AUTO_LOCK, true)
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                task.id + 2000,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            
            Log.d("AlarmScheduler", "Scheduled alarm clock for task ${task.id} at $triggerTime")
            
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling alarm", e)
            // 備用方案：嘗試使用 setExactAndAllowWhileIdle
            try {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "Used setExactAndAllowWhileIdle as fallback")
                }
            } catch (e2: Exception) {
                Log.e("AlarmScheduler", "Failed to schedule alarm", e2)
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
        Log.d("AlarmScheduler", "Cancelled alarm for task ${task.id}")
    }
}