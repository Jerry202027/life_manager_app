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
import android.graphics.Typeface
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.lifemanager.MainActivity
import com.example.lifemanager.alarm.TaskAlarmReceiver

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var lockView: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTaskId: Int = -1
    private var taskDurationMinutes: Long = 30L
    private var taskTitle: String = "任務"
    
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeTextView: TextView? = null

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
        taskDurationMinutes = intent?.getLongExtra(TaskAlarmReceiver.EXTRA_TASK_DURATION_MINUTES, 30L) ?: 30L
        taskTitle = intent?.getStringExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE) ?: "任務"
        
        Log.d(TAG, "Task ID: $currentTaskId, Duration: $taskDurationMinutes min, Title: $taskTitle")
        
        startForeground(FOREGROUND_NOTIFICATION_ID, createNotification())
        Log.d(TAG, "Foreground service started")
        
        wakeUpScreen()
        acquireWakeLock()

        if (Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Has overlay permission, showing lock window")
            showLockWindow()
            startCountdownTimer()
        } else {
            Log.e(TAG, "No overlay permission!")
        }

        return START_STICKY
    }
    
    private fun startCountdownTimer() {
        val durationMillis = taskDurationMinutes * 60 * 1000L
        
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateRemainingTimeDisplay(millisUntilFinished)
            }
            
            override fun onFinish() {
                Log.d(TAG, "Task time finished, auto-unlocking")
                completeTaskAndUnlock()
            }
        }.start()
        
        Log.d(TAG, "Countdown timer started for $taskDurationMinutes minutes")
    }
    
    private fun updateRemainingTimeDisplay(millisUntilFinished: Long) {
        val hours = millisUntilFinished / (1000 * 60 * 60)
        val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millisUntilFinished % (1000 * 60)) / 1000
        
        val timeText = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        
        remainingTimeTextView?.text = timeText
    }
    
    private fun completeTaskAndUnlock() {
        countDownTimer?.cancel()
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
    
    private fun stopLock() {
        countDownTimer?.cancel()
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
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            setOnTouchListener { _, _ -> true }
        }

        // Main content container
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        
        // Task title
        val titleTextView = TextView(this).apply {
            text = taskTitle
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        contentLayout.addView(titleTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 40
        })
        
        // Lock icon/text
        val lockIconTextView = TextView(this).apply {
            text = "🔒"
            textSize = 64f
            gravity = Gravity.CENTER
        }
        contentLayout.addView(lockIconTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 20
        })
        
        // Status text
        val statusTextView = TextView(this).apply {
            text = "專注中"
            setTextColor(Color.parseColor("#E94560"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        contentLayout.addView(statusTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 60
        })
        
        // Remaining time label
        val remainingLabelTextView = TextView(this).apply {
            text = "剩餘時間"
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 16f
            gravity = Gravity.CENTER
        }
        contentLayout.addView(remainingLabelTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 10
        })
        
        // Remaining time countdown
        remainingTimeTextView = TextView(this).apply {
            text = formatInitialTime(taskDurationMinutes)
            setTextColor(Color.WHITE)
            textSize = 56f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        contentLayout.addView(remainingTimeTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        
        // Hint text at bottom
        val hintTextView = TextView(this).apply {
            text = "任務完成後將自動解鎖"
            setTextColor(Color.parseColor("#666666"))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        
        val contentParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        frameLayout.addView(contentLayout, contentParams)
        
        val hintParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 100
        }
        frameLayout.addView(hintTextView, hintParams)

        lockView = frameLayout
        setupSystemUiVisibility()
        addLockViewToWindow()
    }
    
    private fun formatInitialTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            String.format("%d:%02d:00", hours, mins)
        } else {
            String.format("%02d:00", mins)
        }
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
        remainingTimeTextView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
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
            .setContentText("$taskTitle - 剩餘 $taskDurationMinutes 分鐘")
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
        private const val SERVICE_WAKE_LOCK_TIMEOUT = 4 * 60 * 60 * 1000L // Extended to 4 hours max
    }
}
