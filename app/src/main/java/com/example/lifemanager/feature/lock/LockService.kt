package com.example.lifemanager.feature.lock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.lifemanager.MainActivity
import com.example.lifemanager.alarm.TaskAlarmReceiver

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var lockView: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTaskId: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_STOP_LOCK -> {
                Log.d(TAG, "Stopping lock")
                stopLock()
                return START_NOT_STICKY
            }
        }

        currentTaskId = intent?.getIntExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1) ?: -1
        Log.d(TAG, "Task ID: $currentTaskId")
        
        startForeground(FOREGROUND_NOTIFICATION_ID, createNotification())
        Log.d(TAG, "Foreground service started")
        
        wakeUpScreen()
        acquireWakeLock()

        if (Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Has overlay permission, showing lock window")
            showLockWindow()
        } else {
            Log.e(TAG, "No overlay permission!")
        }

        return START_STICKY
    }
    
    private fun stopLock() {
        stopLockWindow()
        releaseWakeLock()
        cancelAlarmNotification()
        stopSelf()
    }
    
    private fun wakeUpScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        if (!powerManager.isInteractive) {
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP or 
                PowerManager.ON_AFTER_RELEASE,
                SCREEN_WAKE_LOCK_TAG
            )
            screenWakeLock.acquire(SCREEN_WAKE_LOCK_TIMEOUT)
        }
    }
    
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                SERVICE_WAKE_LOCK_TAG
            )
            wakeLock?.acquire(SERVICE_WAKE_LOCK_TIMEOUT)
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

        val frameLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            setOnTouchListener { _, _ -> true }
        }

        // Lock text
        val textView = TextView(this).apply {
            text = "鎖定中 - 請專注"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        }
        
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        frameLayout.addView(textView, textParams)

        // Unlock button
        val unlockButton = Button(this).apply {
            text = "完成任務 (解鎖)"
            setOnClickListener {
                stopLockWindow()
                releaseWakeLock()
                cancelAlarmNotification()
                
                // Navigate to work log screen
                val intent = Intent(this@LockService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(ACTION_UNLOCK_COMPLETE, true)
                    putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, currentTaskId)
                }
                startActivity(intent)
                
                stopSelf()
            }
        }
        
        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 200
        }
        frameLayout.addView(unlockButton, buttonParams)

        lockView = frameLayout
        setupSystemUiVisibility()
        addLockViewToWindow()
    }
    
    @Suppress("DEPRECATION")
    private fun setupSystemUiVisibility() {
        lockView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
    
    @Suppress("DEPRECATION")
    private fun addLockViewToWindow() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
        }

        try {
            windowManager.addView(lockView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add lock view", e)
        }
    }

    private fun stopLockWindow() {
        lockView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove lock view", e)
            }
        }
        lockView = null
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
        const val ACTION_UNLOCK_COMPLETE = "ACTION_UNLOCK_COMPLETE"
        const val FOREGROUND_NOTIFICATION_ID = 1
        
        private const val SCREEN_WAKE_LOCK_TAG = "LifeManager::ScreenWakeLock"
        private const val SERVICE_WAKE_LOCK_TAG = "LifeManager::LockServiceWakeLock"
        private const val SCREEN_WAKE_LOCK_TIMEOUT = 10 * 1000L
        private const val SERVICE_WAKE_LOCK_TIMEOUT = 60 * 60 * 1000L
    }
}

