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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lifemanager.core.domain.model.Task
import kotlin.math.max

private val HOUR_HEIGHT = 60.dp
private const val TOTAL_HOURS = 25
private val MIN_TASK_HEIGHT = 44.dp // 最小任務高度，確保短任務也能清楚顯示

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val density = LocalDensity.current
    val minHeightPx = with(density) { MIN_TASK_HEIGHT.toPx() }
    val hourHeightPx = with(density) { HOUR_HEIGHT.toPx() }
    
    // 計算任務的欄位配置（處理重疊）
    val taskLayouts = calculateTaskLayouts(tasks, hourHeightPx, minHeightPx)
    
    Box(
        modifier = modifier.height(HOUR_HEIGHT * TOTAL_HOURS)
    ) {
        // Grid lines
        GridLines()
        
        // Task blocks with smart positioning
        taskLayouts.forEach { layout ->
            TaskBlock(
                task = layout.task,
                isCompact = layout.isCompact,
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp)
                    .fillMaxWidth(layout.widthFraction)
                    .offset(x = layout.offsetX, y = layout.offsetY)
                    .height(layout.displayHeight),
                onClick = { onTaskClick(layout.task) }
            )
        }
    }
}

/**
 * 計算每個任務的顯示位置和大小
 */
private fun calculateTaskLayouts(
    tasks: List<Task>,
    hourHeightPx: Float,
    minHeightPx: Float
): List<TaskLayout> {
    if (tasks.isEmpty()) return emptyList()
    
    // 按開始時間排序
    val sortedTasks = tasks.sortedBy { it.scheduledTimeMinutes }
    
    // 追蹤每個欄位的結束時間
    val columnEndTimes = mutableListOf<Float>()
    val taskColumns = mutableMapOf<Task, Int>()
    val overlappingGroups = mutableListOf<MutableSet<Task>>()
    
    for (task in sortedTasks) {
        val taskStartY = (task.scheduledTimeMinutes / 60f) * hourHeightPx
        val actualHeight = (task.plannedDurationMinutes / 60f) * hourHeightPx
        val displayHeight = max(actualHeight, minHeightPx)
        val taskEndY = taskStartY + displayHeight
        
        // 找到可用的欄位
        var column = columnEndTimes.indexOfFirst { it <= taskStartY }
        if (column == -1) {
            column = columnEndTimes.size
            columnEndTimes.add(taskEndY)
        } else {
            columnEndTimes[column] = taskEndY
        }
        taskColumns[task] = column
        
        // 找到重疊的任務群組
        val overlappingGroup = overlappingGroups.find { group ->
            group.any { existing ->
                val existingStartY = (existing.scheduledTimeMinutes / 60f) * hourHeightPx
                val existingActualHeight = (existing.plannedDurationMinutes / 60f) * hourHeightPx
                val existingDisplayHeight = max(existingActualHeight, minHeightPx)
                val existingEndY = existingStartY + existingDisplayHeight
                // 檢查是否重疊
                taskStartY < existingEndY && taskEndY > existingStartY
            }
        }
        
        if (overlappingGroup != null) {
            overlappingGroup.add(task)
        } else {
            overlappingGroups.add(mutableSetOf(task))
        }
    }
    
    // 計算每個群組的總欄數
    val groupColumns = mutableMapOf<Task, Int>()
    for (group in overlappingGroups) {
        val maxColumn = group.maxOfOrNull { taskColumns[it] ?: 0 } ?: 0
        val totalColumns = maxColumn + 1
        for (task in group) {
            groupColumns[task] = totalColumns
        }
    }
    
    // 生成佈局結果
    return sortedTasks.map { task ->
        val startMinutes = task.scheduledTimeMinutes
        val actualHeightDp = (task.plannedDurationMinutes / 60f) * HOUR_HEIGHT.value
        val displayHeightDp = max(actualHeightDp, MIN_TASK_HEIGHT.value)
        val isCompact = actualHeightDp < MIN_TASK_HEIGHT.value
        
        val column = taskColumns[task] ?: 0
        val totalColumns = groupColumns[task] ?: 1
        val widthFraction = (1f / totalColumns) - 0.01f
        val offsetXDp = (column.toFloat() / totalColumns) * 100f // 簡化的 X 偏移計算
        
        TaskLayout(
            task = task,
            offsetX = offsetXDp.dp,
            offsetY = ((startMinutes / 60f) * HOUR_HEIGHT.value).dp,
            displayHeight = displayHeightDp.dp,
            widthFraction = widthFraction,
            isCompact = isCompact
        )
    }
}

private data class TaskLayout(
    val task: Task,
    val offsetX: Dp,
    val offsetY: Dp,
    val displayHeight: Dp,
    val widthFraction: Float,
    val isCompact: Boolean
)

@Composable
private fun GridLines() {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        
        for (hour in 0..24) {
            val y = hour * HOUR_HEIGHT.toPx()
            
            // Full hour line
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            
            // Half hour line
            if (hour < 24) {
                val yHalf = y + (HOUR_HEIGHT.toPx() / 2)
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(0f, yHalf),
                    end = Offset(width, yHalf),
                    strokeWidth = 1f
                )
            }
        }
    }
}

