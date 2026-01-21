package com.example.lifemanager

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

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called")
        
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID")
            return
        }
        
        Log.d(TAG, "Processing alarm for task: $taskId")

        // 1. 獲取 WakeLock 以確保設備不會在處理過程中休眠
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            PowerManager.ON_AFTER_RELEASE,
            "LifeManager::TaskAlarmWakeLock"
        )
        wakeLock.acquire(60 * 1000L) // 最多持有 60 秒
        
        try {
            // 2. 創建高優先級通知頻道（用於全螢幕 Intent）
            createAlarmNotificationChannel(context)
            
            // 3. 先發送帶有 Full-Screen Intent 的通知
            // 這是讓 app 在背景時也能顯示全螢幕畫面的關鍵
            // 必須在啟動服務之前發送，這樣 Full-Screen Intent 才會正確觸發
            showFullScreenNotification(context, taskId)
            Log.d(TAG, "Full screen notification sent")
            
            // 4. 啟動前台服務並顯示鎖定畫面 overlay
            try {
                val serviceIntent = Intent(context, LockService::class.java).apply {
                    action = LockService.ACTION_START_LOCK
                    putExtra(EXTRA_TASK_ID, taskId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ 需要特殊處理
                    // setAlarmClock 觸發的 BroadcastReceiver 是允許啟動前台服務的
                    context.startForegroundService(serviceIntent)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "LockService started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LockService", e)
                // 如果服務啟動失敗，至少通知已經發送了
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        } finally {
            // WakeLock 會在一段時間後自動釋放
        }
    }
    
    private fun createAlarmNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "任務提醒",
                NotificationManager.IMPORTANCE_HIGH  // 高優先級才能顯示全螢幕 Intent
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
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setBypassDnd(true)  // 繞過勿擾模式
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun showFullScreenNotification(context: Context, taskId: Int) {
        // 創建點擊通知時要開啟的 Intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(ACTION_AUTO_LOCK, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 創建全螢幕 Intent（當螢幕關閉時會直接顯示這個 Activity）
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(ACTION_AUTO_LOCK, true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId + 1000, // 使用不同的 request code
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 建立通知
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("⏰ 任務時間到！")
            .setContentText("點擊開始專注模式")
            .setPriority(NotificationCompat.PRIORITY_MAX)  // 最高優先級
            .setCategory(NotificationCompat.CATEGORY_ALARM)  // 鬧鐘類別
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)  // 關鍵：全螢幕 Intent
            .setAutoCancel(true)
            .setOngoing(false)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + taskId, notification)
    }

    companion object {
        private const val TAG = "TaskAlarmReceiver"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val ACTION_AUTO_LOCK = "ACTION_AUTO_LOCK"
        const val ALARM_CHANNEL_ID = "TaskAlarmChannel"
        const val NOTIFICATION_ID_BASE = 10000
    }
}