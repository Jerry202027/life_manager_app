package com.example.lifemanager.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.feature.lock.LockScreen
import com.example.lifemanager.feature.lock.LockService
import com.example.lifemanager.feature.schedule.ScheduleScreen
import com.example.lifemanager.feature.schedule.ScheduleViewModel
import com.example.lifemanager.feature.taskdetail.TaskDetailScreen
import com.example.lifemanager.feature.worklog.WorkLogScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Event data classes
data class AutoLockEvent(val taskId: Int, val timestamp: Long = System.currentTimeMillis())
data class UnlockCompleteEvent(val taskId: Int, val timestamp: Long = System.currentTimeMillis())

@Composable
fun LifeManagerNavGraph(
    navController: NavHostController,
    viewModel: ScheduleViewModel = hiltViewModel(),
    autoLockEvent: StateFlow<AutoLockEvent?>,
    onAutoLockConsumed: () -> Unit,
    unlockCompleteEvent: StateFlow<UnlockCompleteEvent?>,
    onUnlockCompleteConsumed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    
    var currentTask by remember { mutableStateOf<Task?>(null) }
    
    // Handle auto-lock event
    val autoLockEventValue by autoLockEvent.collectAsStateWithLifecycle()
    LaunchedEffect(autoLockEventValue) {
        autoLockEventValue?.let { event ->
            val task = viewModel.getTaskById(event.taskId)
            task?.let { 
                currentTask = it
                startLocking(context, activity, it, viewModel, navController)
            }
            onAutoLockConsumed()
        }
    }
    
    // Handle unlock complete event
    val unlockCompleteEventValue by unlockCompleteEvent.collectAsStateWithLifecycle()
    LaunchedEffect(unlockCompleteEventValue) {
        unlockCompleteEventValue?.let { event ->
            val task = viewModel.getTaskById(event.taskId)
            task?.let { 
                currentTask = it
                try { activity?.stopLockTask() } catch (e: Exception) { e.printStackTrace() }
                navController.navigate(Routes.WORK_LOG) { 
                    popUpTo(Routes.SCHEDULE) { inclusive = false }
                }
            }
            onUnlockCompleteConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SCHEDULE
    ) {
        composable(Routes.SCHEDULE) {
            ScheduleScreen(
                viewModel = viewModel,
                onNavigateToTaskDetail = { task ->
                    currentTask = task
                    navController.navigate(Routes.TASK_DETAIL)
                }
            )
        }
        
        composable(Routes.LOCK_SCREEN) {
            LockScreen(
                task = currentTask,
                onUnlock = {
                    stopLockService(context)
                    try { activity?.stopLockTask() } catch (e: Exception) { e.printStackTrace() }
                    navController.navigate(Routes.WORK_LOG) {
                        popUpTo(Routes.LOCK_SCREEN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.WORK_LOG) {
            WorkLogScreen(
                onSubmit = { log ->
                    currentTask?.let { task ->
                        coroutineScope.launch {
                            viewModel.completeTask(task, log)
                        }
                    }
                    navController.navigate(Routes.SCHEDULE) {
                        popUpTo(Routes.SCHEDULE) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.TASK_DETAIL) {
            TaskDetailScreen(
                task = currentTask,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private suspend fun startLocking(
    context: Context,
    activity: Activity?,
    task: Task,
    viewModel: ScheduleViewModel,
    navController: NavHostController
) {
    if (!Settings.canDrawOverlays(context)) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    } else {
        val serviceIntent = Intent(context, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        activity?.startLockTask()
        viewModel.startTask(task)
        navController.navigate(Routes.LOCK_SCREEN) { launchSingleTop = true }
    }
}

private fun stopLockService(context: Context) {
    val serviceIntent = Intent(context, LockService::class.java).apply {
        action = LockService.ACTION_STOP_LOCK
    }
    context.startService(serviceIntent)
}
