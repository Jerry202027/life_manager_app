package com.example.lifemanager

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var lockView: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTaskId: Int = -1

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        if (intent?.action == ACTION_STOP_LOCK) {
            android.util.Log.d(TAG, "Stopping lock")
            stopLockWindow()
            releaseWakeLock()
            cancelAlarmNotification()
            stopSelf()
            return START_NOT_STICKY
        }

        // 獲取任務 ID
        currentTaskId = intent?.getIntExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1) ?: -1
        android.util.Log.d(TAG, "Task ID: $currentTaskId")
        
        // 創建通知並啟動前台服務
        val notification = createNotification()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        android.util.Log.d(TAG, "Foreground service started")
        
        // 喚醒螢幕
        wakeUpScreen()
        android.util.Log.d(TAG, "Screen wake up called")
        
        // 獲取 WakeLock 保持設備喚醒
        acquireWakeLock()

        // 檢查是否有 overlay 權限，如果有則顯示鎖定畫面
        if (Settings.canDrawOverlays(this)) {
            android.util.Log.d(TAG, "Has overlay permission, showing lock window")
            showLockWindow()
        } else {
            android.util.Log.e(TAG, "No overlay permission!")
        }

        return START_STICKY
    }
    
    private fun wakeUpScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // 檢查螢幕是否關閉
        if (!powerManager.isInteractive) {
            // 使用 WakeLock 喚醒螢幕
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP or 
                PowerManager.ON_AFTER_RELEASE,
                "LifeManager::ScreenWakeLock"
            )
            screenWakeLock.acquire(10 * 1000L)  // 持有 10 秒足夠喚醒螢幕
        }
    }
    
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            // 使用 PARTIAL_WAKE_LOCK 保持 CPU 運行（不會耗電太多）
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LifeManager::LockServiceWakeLock"
            )
            wakeLock?.acquire(60 * 60 * 1000L) // 最多持有 1 小時
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    private fun cancelAlarmNotification() {
        if (currentTaskId != -1) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(TaskAlarmReceiver.NOTIFICATION_ID_BASE + currentTaskId)
        }
    }

    private fun showLockWindow() {
        if (lockView != null) return

        // 建立全螢幕遮罩 View
        val frameLayout = android.widget.FrameLayout(this)
        frameLayout.setBackgroundColor(android.graphics.Color.BLACK)
        
        // 攔截所有觸控事件，不讓其穿透到底層
        frameLayout.setOnTouchListener { _, _ -> true }

        val textView = android.widget.TextView(this)
        textView.text = "鎖定中 - 請專注"
        textView.setTextColor(android.graphics.Color.WHITE)
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        
        val paramsText = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        paramsText.gravity = Gravity.CENTER
        frameLayout.addView(textView, paramsText)

        // 解鎖按鈕（開發用 - 完成任務）
        val btnUnlock = android.widget.Button(this)
        btnUnlock.text = "完成任務 (解鎖)"
        btnUnlock.setOnClickListener {
            // 停止鎖定畫面
            stopLockWindow()
            releaseWakeLock()
            cancelAlarmNotification()
            
            // 啟動 MainActivity 並導航到任務紀錄頁面
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ACTION_UNLOCK_COMPLETE, true)
                putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, currentTaskId)
            }
            startActivity(intent)
            
            stopSelf()
        }
        val paramsBtn = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        paramsBtn.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        paramsBtn.bottomMargin = 200 // 提高一點，避開可能隱藏的 nav bar 區域
        frameLayout.addView(btnUnlock, paramsBtn)

        lockView = frameLayout

        // 設定 System UI Visibility (沉浸式模式，隱藏狀態列與導航列)
        lockView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // 隱藏 nav bar
            or View.SYSTEM_UI_FLAG_FULLSCREEN // 隱藏 status bar
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 即使滑動也不會輕易出現
        )

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            
            // 關鍵 Flags：
            // FLAG_LAYOUT_IN_SCREEN: 視窗佔據整個螢幕
            // FLAG_LAYOUT_NO_LIMITS: 允許視窗延伸到螢幕邊界外
            // FLAG_NOT_FOCUSABLE: 不獲取焦點（讓通知等可以正常顯示）- 移除以攔截觸控
            // FLAG_SHOW_WHEN_LOCKED: 在鎖定畫面上顯示
            // FLAG_TURN_SCREEN_ON: 打開螢幕
            // FLAG_KEEP_SCREEN_ON: 保持螢幕開啟
            @Suppress("DEPRECATION")
            (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD),
            
            PixelFormat.TRANSLUCENT
        )
        
        // 確保視窗佔滿整個螢幕空間
        layoutParams.gravity = Gravity.FILL

        try {
            windowManager.addView(lockView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopLockWindow() {
        if (lockView != null) {
            try {
                windowManager.removeView(lockView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lockView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLockWindow()
        releaseWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lock Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("專注模式運行中")
            .setContentText("您的手機目前處於鎖定狀態")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "LockService"
        const val CHANNEL_ID = "LockServiceChannel"
        const val ACTION_START_LOCK = "START_LOCK"
        const val ACTION_STOP_LOCK = "STOP_LOCK"
        const val ACTION_UNLOCK_COMPLETE = "ACTION_UNLOCK_COMPLETE"  // 解鎖完成，導航到任務紀錄
        const val FOREGROUND_NOTIFICATION_ID = 1
    }
}