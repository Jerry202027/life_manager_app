package com.example.lifemanager.feature.schedule

import com.example.lifemanager.core.domain.model.Task

/**
 * UI State for Schedule Screen
 * Represents all possible states of the UI
 */
data class ScheduleUiState(
    val tasks: List<Task> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val showAddTaskDialog: Boolean = false,
    val error: String? = null
)

/**
 * UI Events that can be triggered from Schedule Screen
 * One-time events that the ViewModel needs to handle
 */
sealed interface ScheduleUiEvent {
    data class SelectDate(val date: Long) : ScheduleUiEvent
    data object ShowAddTaskDialog : ScheduleUiEvent
    data object HideAddTaskDialog : ScheduleUiEvent
    data class CreateTask(
        val title: String,
        val durationMinutes: Long,
        val scheduledDate: Long,
        val scheduledTimeMinutes: Int,
        val color: Int
    ) : ScheduleUiEvent
    data class TaskClick(val task: Task) : ScheduleUiEvent
    data object ClearError : ScheduleUiEvent
}

/**
 * Side effects that should be handled by the UI layer
 * One-time actions like navigation or showing snackbar
 */
sealed interface ScheduleSideEffect {
    data class NavigateToTaskDetail(val task: Task) : ScheduleSideEffect
    data class ShowSnackbar(val message: String) : ScheduleSideEffect
    data class TaskCreated(val task: Task) : ScheduleSideEffect
}

