package com.example.lifemanager.feature.schedule.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifemanager.core.designsystem.theme.TaskColorOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, duration: Long, date: Long, timeMinutes: Int, color: Int) -> Unit
) {
    val context = LocalContext.current
    
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var selectedDateTime by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedColor by remember { mutableStateOf(TaskColorOptions[0]) }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val timePickerDialog = remember(selectedDateTime) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedDateTime = (selectedDateTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            true
        )
    }

    val datePickerDialog = remember(selectedDateTime) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDateTime = (selectedDateTime.clone() as Calendar).apply {
                    set(year, month, dayOfMonth)
                }
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增日程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任務名稱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = dateFormat.format(selectedDateTime.time))
                    }
                    Button(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = timeFormat.format(selectedDateTime.time))
                    }
                }
                
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("時長 (分鐘)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Color picker - 分成兩排均勻顯示
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 第一排 4 個顏色
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TaskColorOptions.take(4).forEach { color ->
                            ColorPickerItem(
                                color = color,
                                isSelected = selectedColor == color,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                    // 第二排 4 個顏色
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TaskColorOptions.drop(4).forEach { color ->
                            ColorPickerItem(
                                color = color,
                                isSelected = selectedColor == color,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDateTime.before(Calendar.getInstance())) {
                        Toast.makeText(context, "不能在過去的時間新增任務", Toast.LENGTH_SHORT).show()
                    } else if (title.isNotBlank()) {
                        val dateTimestamp = (selectedDateTime.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val timeMinutes = selectedDateTime.get(Calendar.HOUR_OF_DAY) * 60 +
                                selectedDateTime.get(Calendar.MINUTE)
                        
                        onConfirm(
                            title,
                            duration.toLongOrNull() ?: 60,
                            dateTimestamp,
                            timeMinutes,
                            selectedColor.toArgb()
                        )
                    }
                }
            ) {
                Text("新增")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ColorPickerItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
    )
}

