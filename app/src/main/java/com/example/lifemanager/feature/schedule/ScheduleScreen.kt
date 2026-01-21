package com.example.lifemanager.feature.schedule

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.feature.schedule.component.AddTaskDialog
import com.example.lifemanager.feature.schedule.component.DateHeader
import com.example.lifemanager.feature.schedule.component.TimeTable
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateToTaskDetail: (Task) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Request exact alarm permission if needed
    RequestExactAlarmPermission(context)
    
    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is ScheduleSideEffect.NavigateToTaskDetail -> {
                    onNavigateToTaskDetail(effect.task)
                }
                is ScheduleSideEffect.ShowSnackbar -> {
                    // TODO: Show snackbar
                }
                is ScheduleSideEffect.TaskCreated -> {
                    // Task created successfully, dialog already closed
                }
            }
        }
    }

    Scaffold(
        topBar = {
            DateHeader(
                selectedDate = uiState.selectedDate,
                onDateChange = { viewModel.onEvent(ScheduleUiEvent.SelectDate(it)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ScheduleUiEvent.ShowAddTaskDialog) },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增任務",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { padding ->
        TimeTable(
            tasks = uiState.tasks,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onTaskClick = { viewModel.onEvent(ScheduleUiEvent.TaskClick(it)) }
        )
    }

    // Add Task Dialog
    if (uiState.showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { viewModel.onEvent(ScheduleUiEvent.HideAddTaskDialog) },
            onConfirm = { title, duration, date, timeMinutes, color ->
                viewModel.onEvent(
                    ScheduleUiEvent.CreateTask(
                        title = title,
                        durationMinutes = duration,
                        scheduledDate = date,
                        scheduledTimeMinutes = timeMinutes,
                        color = color
                    )
                )
            }
        )
    }
}

@Composable
private fun RequestExactAlarmPermission(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        LaunchedEffect(Unit) {
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                context.startActivity(intent)
            }
        }
    }
}

