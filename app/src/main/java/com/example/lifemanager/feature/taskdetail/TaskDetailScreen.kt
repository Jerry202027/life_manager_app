package com.example.lifemanager.feature.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.core.domain.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskDetailScreen(
    task: Task?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Button(onClick = onBack) {
            Text("← 返回")
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (task != null) {
            TaskDetailContent(task = task)
        } else {
            Text("找不到任務資料")
        }
    }
}

@Composable
private fun TaskDetailContent(task: Task) {
    val isAfterScheduledTime = task.isPastScheduledTime()
    
    // Task header
    TaskHeader(task = task)
    
    Spacer(modifier = Modifier.height(20.dp))
    
    // Status display
    TaskStatusSection(task = task, isAfterScheduledTime = isAfterScheduledTime)
    
    // Work log section (shown after scheduled time or when completed)
    if (isAfterScheduledTime || task.status == TaskStatus.COMPLETED) {
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        WorkLogSection(task = task)
    }
}

@Composable
private fun TaskHeader(task: Task) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(task.color), RoundedCornerShape(8.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                text = "預定時間: ${task.getTimeRangeString()}",
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "預計時長: ${task.plannedDurationMinutes} 分鐘",
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun TaskStatusSection(task: Task, isAfterScheduledTime: Boolean) {
    when (task.status) {
        TaskStatus.COMPLETED -> {
            Text(
                text = "狀態：✅ 已完成",
                color = Color(0xFF2E7D32),
                fontSize = 20.sp
            )
        }
        TaskStatus.IN_PROGRESS -> {
            Text(
                text = "狀態：🔒 進行中",
                color = Color(0xFF1976D2),
                fontSize = 20.sp
            )
        }
        TaskStatus.ABANDONED -> {
            Text(
                text = "狀態：❌ 已放棄",
                color = Color(0xFFD32F2F),
                fontSize = 20.sp
            )
        }
        TaskStatus.PLANNED -> {
            if (isAfterScheduledTime) {
                Text(
                    text = "狀態：⏰ 已過預定時間 (尚未執行)",
                    color = Color(0xFFFF9800),
                    fontSize = 20.sp
                )
            } else {
                Text(
                    text = "狀態：📋 計劃中",
                    color = Color.Gray,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                RemainingTimeText(task = task)
            }
        }
    }
}

@Composable
private fun RemainingTimeText(task: Task) {
    val remainingMillis = task.scheduledStartTimeMillis - System.currentTimeMillis()
    val remainingMinutes = (remainingMillis / 1000 / 60).toInt()
    val remainingHours = remainingMinutes / 60
    val remainingMins = remainingMinutes % 60
    
    val text = if (remainingHours > 0) {
        "距離開始還有 $remainingHours 小時 $remainingMins 分鐘"
    } else {
        "距離開始還有 $remainingMins 分鐘"
    }
    
    Text(text = text, color = Color.Gray)
}

@Composable
private fun WorkLogSection(task: Task) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Text(
        text = "📝 任務紀錄",
        style = MaterialTheme.typography.titleMedium
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Actual time records
    task.startTime?.let {
        Text(
            text = "實際開始: ${timeFormat.format(Date(it))}",
            color = Color.Gray
        )
    }
    
    task.endTime?.let {
        Text(
            text = "實際結束: ${timeFormat.format(Date(it))}",
            color = Color.Gray
        )
    }
    
    task.getActualDurationMinutes()?.let {
        Text(
            text = "實際耗時: $it 分鐘",
            color = Color.Gray
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Work log content
    Text(
        text = "工作心得:",
        style = MaterialTheme.typography.titleSmall
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = task.workLog ?: "（尚無紀錄）",
            modifier = Modifier.padding(16.dp),
            color = if (task.workLog == null) Color.Gray else MaterialTheme.colorScheme.onSurface
        )
    }
}

