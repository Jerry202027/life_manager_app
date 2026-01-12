package com.example.lifemanager

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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var lockView: View? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_LOCK) {
            stopLockWindow()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        startForeground(1, notification)

        showLockWindow()

        return START_STICKY
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

        // 緊急解鎖按鈕
        val btnUnlock = android.widget.Button(this)
        btnUnlock.text = "緊急解鎖 (開發用)"
        btnUnlock.setOnClickListener {
            stopLockWindow()
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
                WindowManager.LayoutParams.TYPE_PHONE,
            
            // 關鍵 Flags 修改：
            // FLAG_LAYOUT_NO_LIMITS: 允許視窗延伸到螢幕邊界外 (覆蓋狀態列/導航列)
            // FLAG_NOT_TOUCH_MODAL: 允許我們接收視窗外的 touch? 不，我們要全擋。
            // FLAG_SHOW_WHEN_LOCKED: 即使螢幕鎖定也要顯示 (選用)
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            
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
        const val CHANNEL_ID = "LockServiceChannel"
        const val ACTION_START_LOCK = "START_LOCK"
        const val ACTION_STOP_LOCK = "STOP_LOCK"
    }
}