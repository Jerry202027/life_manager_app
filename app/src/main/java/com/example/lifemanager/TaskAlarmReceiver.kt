package com.example.lifemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        // 1. 帶起主畫面，讓使用者知道發生了什麼事
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 可以帶參數通知 MainActivity 要直接鎖定某個 task
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(ACTION_AUTO_LOCK, true)
        }
        context.startActivity(mainActivityIntent)
    }

    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val ACTION_AUTO_LOCK = "ACTION_AUTO_LOCK"
    }
}