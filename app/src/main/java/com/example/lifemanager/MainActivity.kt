package com.example.lifemanager

import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lifemanager.data.AppDatabase
import com.example.lifemanager.data.Task
import com.example.lifemanager.data.TaskRepository
import com.example.lifemanager.data.TaskStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        alarmScheduler = AlarmSchedulerImpl(this)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())
        val viewModelFactory = LifeManagerViewModelFactory(repository)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LifeManagerApp(viewModelFactory, alarmScheduler)
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        // This function is now mainly for logging or pre-processing if needed.
        // The main logic is handled by LaunchedEffect in the Composable.
    }
}

object Routes {
    const val HOME = "home"
    const val LOCK_SCREEN = "lock_screen"
    const val WORK_LOG = "work_log"
    const val TASK_DETAIL = "task_detail"
}

@Composable
fun LifeManagerApp(viewModelFactory: LifeManagerViewModelFactory, alarmScheduler: AlarmScheduler) {
    val navController = rememberNavController()
    val viewModel: LifeManagerViewModel = viewModel(factory = viewModelFactory)
    val context = LocalContext.current
    val activity = context as? Activity
    
    var currentTask by remember { mutableStateOf<Task?>(null) }
    
    fun startLocking(task: Task) {
        currentTask = task
        if (!Settings.canDrawOverlays(context)) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
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

    LaunchedEffect(Unit) {
        val intent = (context as? Activity)?.intent
        if (intent?.getBooleanExtra(TaskAlarmReceiver.ACTION_AUTO_LOCK, false) == true) {
            val taskId = intent.getIntExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1)
            if (taskId != -1) {
                val task = viewModel.getTaskById(taskId)
                task?.let { 
                    intent.removeExtra(TaskAlarmReceiver.ACTION_AUTO_LOCK)
                    startLocking(it)
                 }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            ScheduleScreen(
                viewModel = viewModel,
                alarmScheduler = alarmScheduler,
                onTaskClick = { task ->
                    currentTask = task
                    if (task.status == TaskStatus.COMPLETED) {
                        navController.navigate(Routes.TASK_DETAIL)
                    } else {
                        startLocking(task)
                    }
                }
            )
        }
        composable(Routes.LOCK_SCREEN) {
            LockScreen(
                task = currentTask,
                onTimeUp = {
                    val serviceIntent = Intent(context, LockService::class.java)
                    serviceIntent.action = LockService.ACTION_STOP_LOCK
                    context.startService(serviceIntent)
                    try { activity?.stopLockTask() } catch (e: Exception) { e.printStackTrace() }
                    navController.navigate(Routes.WORK_LOG) { popUpTo(Routes.LOCK_SCREEN) { inclusive = true } }
                }
            )
        }
        composable(Routes.WORK_LOG) {
            WorkLogScreen(
                onSubmit = { log ->
                    currentTask?.let { task -> viewModel.completeTask(task, log) }
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
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

@Composable
fun ScheduleScreen(
    viewModel: LifeManagerViewModel,
    alarmScheduler: AlarmScheduler,
    onTaskClick: (Task) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tasks by viewModel.tasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        LaunchedEffect(Unit) {
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        topBar = { DateHeader(selectedDate = selectedDate, onDateChange = { viewModel.selectDate(it) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.size(64.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新增任務", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        TimeTableLayout(tasks = tasks, modifier = Modifier.padding(padding).fillMaxSize(), onTaskClick = onTaskClick)
    }

    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, duration, calendar, color ->
                coroutineScope.launch {
                    val dateTimestamp = (calendar.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val timeMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                    
                    val createdTask = viewModel.createTask(title, duration, dateTimestamp, timeMinutes, color)
                    alarmScheduler.schedule(createdTask)
                    showDialog = false
                }
            }
        )
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Calendar, Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    
    val currentCalendar = Calendar.getInstance()
    var selectedDateTime by remember { mutableStateOf(currentCalendar) }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedDateTime = (selectedDateTime.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, hourOfDay); set(Calendar.MINUTE, minute) }
        },
        selectedDateTime.get(Calendar.HOUR_OF_DAY),
        selectedDateTime.get(Calendar.MINUTE),
        true
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDateTime = (selectedDateTime.clone() as Calendar).apply { set(year, month, dayOfMonth) }
        },
        selectedDateTime.get(Calendar.YEAR),
        selectedDateTime.get(Calendar.MONTH),
        selectedDateTime.get(Calendar.DAY_OF_MONTH)
    )

    val colors = listOf(Color(0xFF2196F3), Color(0xFFF44336), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800))
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增日程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("任務名稱") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { datePickerDialog.show() }, modifier = Modifier.weight(1f)) { Text(text = dateFormat.format(selectedDateTime.time)) }
                    Button(onClick = { timePickerDialog.show() }, modifier = Modifier.weight(1f)) { Text(text = timeFormat.format(selectedDateTime.time)) }
                }
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("時長 (分鐘)") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .border(width = 3.dp, color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent, shape = CircleShape)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedDateTime.before(Calendar.getInstance())) {
                    Toast.makeText(context, "不能在過去的時間新增任務", Toast.LENGTH_SHORT).show()
                } else {
                    if (title.isNotBlank()) {
                        val colorInt = selectedColor.value.toLong().and(0xFFFFFFFF).toInt()
                        onConfirm(title, duration.toLongOrNull() ?: 60, selectedDateTime, colorInt)
                    }
                }
            }) { Text("新增") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun DateHeader(selectedDate: Long, onDateChange: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate
    val dateFormat = SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { calendar.add(Calendar.DAY_OF_YEAR, -1); onDateChange(calendar.timeInMillis) }) { Text("<") }
        Text(dateFormat.format(Date(selectedDate)), style = MaterialTheme.typography.titleLarge)
        Button(onClick = { calendar.add(Calendar.DAY_OF_YEAR, 1); onDateChange(calendar.timeInMillis) }) { Text(">") }
    }
}

@Composable
fun TimeTableLayout(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onTaskClick: (Task) -> Unit
) {
    val scrollState = rememberScrollState()
    val hourHeight = 60.dp
    
    Row(modifier = modifier.verticalScroll(scrollState)) {
        Column(modifier = Modifier.width(60.dp)) {
            for (i in 0..24) {
                Box(modifier = Modifier.height(hourHeight), contentAlignment = Alignment.TopCenter) {
                    Text(text = "%02d:00".format(i), style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Box(modifier = Modifier.weight(1f).height(hourHeight * 25)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                for (i in 0..24) {
                    val y = i * hourHeight.toPx()
                    drawLine(color = Color.LightGray, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    if (i < 24) {
                        val yHalf = y + (hourHeight.toPx() / 2)
                        drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(0f, yHalf), end = Offset(width, yHalf), strokeWidth = 1f)
                    }
                }
            }
            tasks.forEach { task ->
                val startMinutes = task.scheduledTimeMinutes
                val duration = task.plannedDurationMinutes
                val offsetTop = (startMinutes / 60f) * hourHeight.value
                val itemHeight = (duration / 60f) * hourHeight.value
                TaskBlock(
                    task = task,
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp).fillMaxWidth().offset(y = offsetTop.dp).height(itemHeight.dp),
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
fun TaskBlock(task: Task, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor = Color(task.color)
    val contentColor = if (backgroundColor.luminance() > 0.5) Color.Black else Color.White

    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleSmall, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val startHour = task.scheduledTimeMinutes / 60
            val startMin = task.scheduledTimeMinutes % 60
            val endTotal = task.scheduledTimeMinutes + task.plannedDurationMinutes
            val endHour = (endTotal / 60) % 24
            val endMin = endTotal % 60
            Text(text = "%02d:%02d - %02d:%02d".format(startHour, startMin, endHour, endMin), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun TaskDetailScreen(task: Task?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("← 返回") }
        Spacer(modifier = Modifier.height(20.dp))
        if (task != null) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(task.color), RoundedCornerShape(8.dp)).padding(20.dp)) {
                Column {
                    Text(text = task.title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    val startH = task.scheduledTimeMinutes / 60
                    val startM = task.scheduledTimeMinutes % 60
                    Text(text = "預計: %02d:%02d (%d分鐘)".format(startH, startM, task.plannedDurationMinutes), color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (task.status == TaskStatus.COMPLETED) {
                Text(text = "狀態：✅ 已完成", color = Color(0xFF2E7D32), fontSize = 20.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "工作紀錄 (Log):", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(text = task.workLog ?: "（無紀錄）", modifier = Modifier.padding(16.dp))
                }
                 if (task.startTime != null && task.endTime != null) {
                    val duration = (task.endTime - task.startTime) / 1000 / 60
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("實際耗時: $duration 分鐘")
                }
            } else {
                Text(text = "狀態：尚未完成", color = Color.Gray)
            }
        } else {
            Text("找不到任務資料")
        }
    }
}

@Composable
fun LockScreen(task: Task?, onTimeUp: () -> Unit) {
    BackHandler { }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🔒 專注模式", color = Color.White, fontSize = 32.sp)
        task?.let { Text("正在進行: ${it.title}", color = Color.LightGray, fontSize = 24.sp) }
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onTimeUp, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("結束專注 (解鎖)")
        }
    }
}

@Composable
fun WorkLogScreen(onSubmit: (String) -> Unit) {
    BackHandler { }
    var logText by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("📝 任務回顧", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(value = logText, onValueChange = { logText = it }, label = { Text("輸入工作紀錄...") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { onSubmit(logText) }, enabled = logText.isNotEmpty()) {
            Text("提交並完全解鎖")
        }
    }
}
