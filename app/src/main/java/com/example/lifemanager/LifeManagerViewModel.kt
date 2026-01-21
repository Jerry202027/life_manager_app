package com.example.lifemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lifemanager.data.Task
import com.example.lifemanager.data.TaskRepository
import com.example.lifemanager.data.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class LifeManagerViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getTodayTimestamp())
    val selectedDate = _selectedDate.asStateFlow()

    val tasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date ->
            repository.getTasksForDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

    // MODIFIED: This is now a suspend function that returns the created Task
    suspend fun createTask(title: String, duration: Long, date: Long, timeMinutes: Int, color: Int): Task {
        // Use withContext to switch to an I/O thread for database operations
        return withContext(Dispatchers.IO) {
            repository.addTask(title, duration, date, timeMinutes, color)
        }
    }

    fun startTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(
                status = TaskStatus.IN_PROGRESS,
                startTime = System.currentTimeMillis()
            )
            repository.updateTask(updatedTask)
        }
    }

    fun completeTask(task: Task, log: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(
                status = TaskStatus.COMPLETED,
                endTime = System.currentTimeMillis(),
                workLog = log
            )
            repository.updateTask(updatedTask)
        }
    }
    
    // ADDED: A suspend function for the UI to get a task by ID for auto-locking
    suspend fun getTaskById(id: Int): Task? {
        return withContext(Dispatchers.IO) {
            repository.getTask(id)
        }
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

class LifeManagerViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifeManagerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}