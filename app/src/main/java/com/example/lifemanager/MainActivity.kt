package com.example.lifemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.lifemanager.alarm.TaskAlarmReceiver
import com.example.lifemanager.core.designsystem.theme.LifeManagerTheme
import com.example.lifemanager.feature.lock.LockService
import com.example.lifemanager.navigation.AutoLockEvent
import com.example.lifemanager.navigation.LifeManagerNavGraph
import com.example.lifemanager.navigation.UnlockCompleteEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val _autoLockEvent = MutableStateFlow<AutoLockEvent?>(null)
    val autoLockEvent: StateFlow<AutoLockEvent?> = _autoLockEvent.asStateFlow()
    
    private val _unlockCompleteEvent = MutableStateFlow<UnlockCompleteEvent?>(null)
    val unlockCompleteEvent: StateFlow<UnlockCompleteEvent?> = _unlockCompleteEvent.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LifeManagerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    LifeManagerNavGraph(
                        navController = navController,
                        autoLockEvent = autoLockEvent,
                        onAutoLockConsumed = { _autoLockEvent.value = null },
                        unlockCompleteEvent = unlockCompleteEvent,
                        onUnlockCompleteConsumed = { _unlockCompleteEvent.value = null }
                    )
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Handle auto-lock event from alarm
        if (intent.getBooleanExtra(TaskAlarmReceiver.ACTION_AUTO_LOCK, false)) {
            val taskId = intent.getIntExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1)
            if (taskId != -1) {
                _autoLockEvent.value = AutoLockEvent(taskId)
                intent.removeExtra(TaskAlarmReceiver.ACTION_AUTO_LOCK)
            }
        }
        
        // Handle unlock complete event from LockService
        if (intent.getBooleanExtra(LockService.ACTION_UNLOCK_COMPLETE, false)) {
            val taskId = intent.getIntExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1)
            if (taskId != -1) {
                _unlockCompleteEvent.value = UnlockCompleteEvent(taskId)
                intent.removeExtra(LockService.ACTION_UNLOCK_COMPLETE)
            }
        }
    }
}
