package com.example.lifemanager.feature.schedule.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DateHeader(
    selectedDate: Long,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = selectedDate
    }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                onDateChange(calendar.timeInMillis)
            }
        ) {
            Text("<")
        }
        
        Text(
            text = dateFormat.format(Date(selectedDate)),
            style = MaterialTheme.typography.titleLarge
        )
        
        Button(
            onClick = {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                onDateChange(calendar.timeInMillis)
            }
        ) {
            Text(">")
        }
    }
}

