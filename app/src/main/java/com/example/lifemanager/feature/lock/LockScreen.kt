package com.example.lifemanager.feature.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifemanager.core.domain.model.Task

@Composable
fun LockScreen(
    task: Task?,
    onUnlock: () -> Unit
) {
    // Disable back button
    BackHandler { }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔒 專注模式",
            color = Color.White,
            fontSize = 32.sp
        )
        
        task?.let {
            Text(
                text = "正在進行: ${it.title}",
                color = Color.LightGray,
                fontSize = 24.sp
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("結束專注 (解鎖)")
        }
    }
}

