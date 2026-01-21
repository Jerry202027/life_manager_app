package com.example.lifemanager.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemanager.alarm.AlarmScheduler
import com.example.lifemanager.core.data.repository.TaskRepository
import com.example.lifemanager.core.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getTodayTimestamp())
    private val _showAddTaskDialog = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    // Side effects channel for one-time events
    private val _sideEffect = Channel<ScheduleSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()
    
    // Combine all state flows into a single UiState
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleUiState> = combine(
        _selectedDate.flatMapLatest { date -> repository.getTasksForDate(date) },
        _selectedDate,
        _showAddTaskDialog,
        _isLoading,
        _error
    ) { tasks, selectedDate, showDialog, isLoading, error ->
        ScheduleUiState(
            tasks = tasks,
            selectedDate = selectedDate,
            showAddTaskDialog = showDialog,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState()
    )
    
    /**
     * Handle UI events
     */
    fun onEvent(event: ScheduleUiEvent) {
        when (event) {
            is ScheduleUiEvent.SelectDate -> selectDate(event.date)
            is ScheduleUiEvent.ShowAddTaskDialog -> showAddTaskDialog()
            is ScheduleUiEvent.HideAddTaskDialog -> hideAddTaskDialog()
            is ScheduleUiEvent.CreateTask -> createTask(event)
            is ScheduleUiEvent.TaskClick -> onTaskClick(event.task)
            is ScheduleUiEvent.ClearError -> clearError()
        }
    }
    
    private fun selectDate(date: Long) {
        _selectedDate.value = date
    }
    
    private fun showAddTaskDialog() {
        _showAddTaskDialog.value = true
    }
    
    private fun hideAddTaskDialog() {
        _showAddTaskDialog.value = false
    }
    
    private fun createTask(event: ScheduleUiEvent.CreateTask) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val task = repository.createTask(
                    title = event.title,
                    durationMinutes = event.durationMinutes,
                    scheduledDate = event.scheduledDate,
                    scheduledTimeMinutes = event.scheduledTimeMinutes,
                    color = event.color
                )
                alarmScheduler.schedule(task)
                _showAddTaskDialog.value = false
                _sideEffect.send(ScheduleSideEffect.TaskCreated(task))
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create task"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun onTaskClick(task: Task) {
        viewModelScope.launch {
            _sideEffect.send(ScheduleSideEffect.NavigateToTaskDetail(task))
        }
    }
    
    private fun clearError() {
        _error.value = null
    }
    
    /**
     * Get a task by ID (for auto-lock flow)
     */
    suspend fun getTaskById(id: Int): Task? {
        return repository.getTaskById(id)
    }
    
    /**
     * Start a task (change status to IN_PROGRESS)
     */
    suspend fun startTask(task: Task): Task {
        return repository.startTask(task)
    }
    
    /**
     * Complete a task with work log
     */
    suspend fun completeTask(task: Task, workLog: String): Task {
        return repository.completeTask(task, workLog)
    }
    
    private fun getTodayTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

