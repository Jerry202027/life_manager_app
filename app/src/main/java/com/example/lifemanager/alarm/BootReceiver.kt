package com.example.lifemanager.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lifemanager.core.data.local.AppDatabase
import com.example.lifemanager.core.data.local.entity.toDomain
import com.example.lifemanager.core.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * BroadcastReceiver to reschedule alarms after device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        Log.d(TAG, "Boot completed, rescheduling alarms")
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAlarms(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun rescheduleAlarms(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()
        val alarmScheduler = AlarmSchedulerImpl(context)
        
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val pendingTasks = taskDao.getPendingTasksFromDate(todayStart, TaskStatus.PLANNED)
        
        var scheduledCount = 0
        for (taskEntity in pendingTasks) {
            val task = taskEntity.toDomain()
            alarmScheduler.schedule(task)
            scheduledCount++
        }
        
        Log.d(TAG, "Rescheduled $scheduledCount alarms")
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}

