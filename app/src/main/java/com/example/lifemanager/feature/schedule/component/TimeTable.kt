package com.example.lifemanager.feature.schedule.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifemanager.core.domain.model.Task

private val HOUR_HEIGHT = 60.dp
private const val TOTAL_HOURS = 25

@Composable
fun TimeTable(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onTaskClick: (Task) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Row(modifier = modifier.verticalScroll(scrollState)) {
        // Time labels column
        TimeLabelsColumn()
        
        // Tasks area
        TasksArea(
            tasks = tasks,
            onTaskClick = onTaskClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeLabelsColumn() {
    Column(modifier = Modifier.width(60.dp)) {
        for (hour in 0..24) {
            Box(
                modifier = Modifier.height(HOUR_HEIGHT),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "%02d:00".format(hour),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TasksArea(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(HOUR_HEIGHT * TOTAL_HOURS)
    ) {
        // Grid lines
        GridLines()
        
        // Task blocks
        tasks.forEach { task ->
            val startMinutes = task.scheduledTimeMinutes
            val duration = task.plannedDurationMinutes
            val offsetTop = (startMinutes / 60f) * HOUR_HEIGHT.value
            val itemHeight = (duration / 60f) * HOUR_HEIGHT.value
            
            TaskBlock(
                task = task,
                modifier = Modifier
                    .padding(start = 4.dp, end = 8.dp)
                    .fillMaxWidth()
                    .offset(y = offsetTop.dp)
                    .height(itemHeight.dp),
                onClick = { onTaskClick(task) }
            )
        }
    }
}

@Composable
private fun GridLines() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        
        for (hour in 0..24) {
            val y = hour * HOUR_HEIGHT.toPx()
            
            // Full hour line
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            
            // Half hour line
            if (hour < 24) {
                val yHalf = y + (HOUR_HEIGHT.toPx() / 2)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, yHalf),
                    end = Offset(width, yHalf),
                    strokeWidth = 1f
                )
            }
        }
    }
}

