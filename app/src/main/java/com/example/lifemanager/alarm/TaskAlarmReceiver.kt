package com.example.lifemanager.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lifemanager.MainActivity
import com.example.lifemanager.feature.lock.LockService

/**
 * BroadcastReceiver for handling task alarm events
 */
class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called")
        
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID")
            return
        }
        
        val taskDurationMinutes = intent.getLongExtra(EXTRA_TASK_DURATION_MINUTES, 30L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "任務"
        
        Log.d(TAG, "Processing alarm for task: $taskId, duration: $taskDurationMinutes min")

        // Acquire wake lock to keep device awake during processing
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        wakeLock.acquire(WAKE_LOCK_TIMEOUT)
        
        try {
            createAlarmNotificationChannel(context)
            showFullScreenNotification(context, taskId, taskTitle)
            Log.d(TAG, "Full screen notification sent")
            
            startLockService(context, taskId, taskDurationMinutes, taskTitle)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        }
    }
    
    private fun createAlarmNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "任務提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "任務時間到達時的提醒通知"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun showFullScreenNotification(context: Context, taskId: Int, taskTitle: String) {
        val contentIntent = createMainActivityIntent(context, taskId)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val fullScreenIntent = createMainActivityIntent(context, taskId).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId + FULL_SCREEN_INTENT_OFFSET,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("⏰ $taskTitle - 時間到！")
            .setContentText("點擊開始專注模式")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(false)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(VIBRATION_PATTERN)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + taskId, notification)
    }
    
    private fun createMainActivityIntent(context: Context, taskId: Int): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(ACTION_AUTO_LOCK, true)
        }
    }
    
    private fun startLockService(context: Context, taskId: Int, durationMinutes: Long, taskTitle: String) {
        try {
            val serviceIntent = Intent(context, LockService::class.java).apply {
                action = LockService.ACTION_START_LOCK
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_DURATION_MINUTES, durationMinutes)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "LockService started with duration: $durationMinutes min")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LockService", e)
        }
    }

    companion object {
        private const val TAG = "TaskAlarmReceiver"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_DURATION_MINUTES = "EXTRA_TASK_DURATION_MINUTES"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val ACTION_AUTO_LOCK = "ACTION_AUTO_LOCK"
        const val ALARM_CHANNEL_ID = "TaskAlarmChannel"
        const val NOTIFICATION_ID_BASE = 10000
        
        private const val WAKE_LOCK_TAG = "LifeManager::TaskAlarmWakeLock"
        private const val WAKE_LOCK_TIMEOUT = 60 * 1000L
        private const val FULL_SCREEN_INTENT_OFFSET = 1000
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500)
    }
}

