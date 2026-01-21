package com.example.lifemanager.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lifemanager.MainActivity
import com.example.lifemanager.core.domain.model.Task
import java.util.Calendar

/**
 * Interface for scheduling task alarms
 */
interface AlarmScheduler {
    fun schedule(task: Task)
    fun cancel(task: Task)
}

/**
 * Implementation of AlarmScheduler using AlarmManager
 */
class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: Task) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.scheduledDate
            add(Calendar.MINUTE, task.scheduledTimeMinutes)
        }

        val triggerTime = calendar.timeInMillis
        
        // Don't schedule if time has passed
        if (triggerTime < System.currentTimeMillis()) {
            Log.d(TAG, "Task ${task.id} time has passed, not scheduling")
            return
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_DURATION_MINUTES, task.plannedDurationMinutes)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            // Use setAlarmClock for reliable alarm delivery
            // This is exempted from Doze mode and background restrictions
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
                putExtra(TaskAlarmReceiver.ACTION_AUTO_LOCK, true)
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                task.id + SHOW_INTENT_OFFSET,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            
            Log.d(TAG, "Scheduled alarm for task ${task.id} at $triggerTime")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm", e)
            // Fallback to setExactAndAllowWhileIdle
            tryFallbackScheduling(triggerTime, pendingIntent)
        }
    }
    
    private fun tryFallbackScheduling(triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Used setExactAndAllowWhileIdle as fallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm with fallback", e)
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
        Log.d(TAG, "Cancelled alarm for task ${task.id}")
    }
    
    companion object {
        private const val TAG = "AlarmScheduler"
        private const val SHOW_INTENT_OFFSET = 2000
    }
}

