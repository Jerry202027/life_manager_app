package com.example.lifemanager.feature.schedule

import app.cash.turbine.test
import com.example.lifemanager.alarm.AlarmScheduler
import com.example.lifemanager.core.data.repository.TaskRepository
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.core.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var repository: TaskRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private val testDispatcher = StandardTestDispatcher()

    private val testTask = Task(
        id = 1,
        title = "Test Task",
        description = "Test Description",
        scheduledDate = System.currentTimeMillis(),
        scheduledTimeMinutes = 600, // 10:00
        plannedDurationMinutes = 60,
        color = 0xFF2196F3.toInt(),
        status = TaskStatus.PLANNED
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)
        
        every { repository.getTasksForDate(any()) } returns flowOf(listOf(testTask))
        
        viewModel = ScheduleViewModel(repository, alarmScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty tasks and today's date`() = runTest {
        val initialState = viewModel.uiState.value
        
        assertFalse(initialState.isLoading)
        assertFalse(initialState.showAddTaskDialog)
        assertEquals(null, initialState.error)
    }

    @Test
    fun `show add task dialog event should update state`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.showAddTaskDialog)
            
            viewModel.onEvent(ScheduleUiEvent.ShowAddTaskDialog)
            
            val updatedState = awaitItem()
            assertTrue(updatedState.showAddTaskDialog)
        }
    }

    @Test
    fun `hide add task dialog event should update state`() = runTest {
        viewModel.onEvent(ScheduleUiEvent.ShowAddTaskDialog)
        
        viewModel.uiState.test {
            skipItems(1) // Skip the current state
            
            viewModel.onEvent(ScheduleUiEvent.HideAddTaskDialog)
            
            val updatedState = awaitItem()
            assertFalse(updatedState.showAddTaskDialog)
        }
    }

    @Test
    fun `create task should call repository and schedule alarm`() = runTest {
        coEvery { 
            repository.createTask(any(), any(), any(), any(), any()) 
        } returns testTask

        viewModel.onEvent(
            ScheduleUiEvent.CreateTask(
                title = "New Task",
                durationMinutes = 60,
                scheduledDate = System.currentTimeMillis(),
                scheduledTimeMinutes = 600,
                color = 0xFF2196F3.toInt()
            )
        )
        
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { 
            repository.createTask(
                title = "New Task",
                durationMinutes = 60,
                scheduledDate = any(),
                scheduledTimeMinutes = 600,
                color = any()
            )
        }
        verify { alarmScheduler.schedule(testTask) }
    }

    @Test
    fun `task click should emit navigate side effect`() = runTest {
        viewModel.sideEffect.test {
            viewModel.onEvent(ScheduleUiEvent.TaskClick(testTask))
            
            testDispatcher.scheduler.advanceUntilIdle()
            
            val effect = awaitItem()
            assertTrue(effect is ScheduleSideEffect.NavigateToTaskDetail)
            assertEquals(testTask, (effect as ScheduleSideEffect.NavigateToTaskDetail).task)
        }
    }

    @Test
    fun `select date should update state`() = runTest {
        val newDate = System.currentTimeMillis() + 86400000L // Tomorrow
        
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onEvent(ScheduleUiEvent.SelectDate(newDate))
            
            val updatedState = awaitItem()
            assertEquals(newDate, updatedState.selectedDate)
        }
    }

    @Test
    fun `get task by id should call repository`() = runTest {
        coEvery { repository.getTaskById(1) } returns testTask

        val result = viewModel.getTaskById(1)

        assertEquals(testTask, result)
        coVerify { repository.getTaskById(1) }
    }

    @Test
    fun `start task should call repository`() = runTest {
        val startedTask = testTask.copy(status = TaskStatus.IN_PROGRESS)
        coEvery { repository.startTask(testTask) } returns startedTask

        val result = viewModel.startTask(testTask)

        assertEquals(startedTask, result)
        coVerify { repository.startTask(testTask) }
    }

    @Test
    fun `complete task should call repository`() = runTest {
        val completedTask = testTask.copy(
            status = TaskStatus.COMPLETED,
            workLog = "Test log"
        )
        coEvery { repository.completeTask(testTask, "Test log") } returns completedTask

        val result = viewModel.completeTask(testTask, "Test log")

        assertEquals(completedTask, result)
        coVerify { repository.completeTask(testTask, "Test log") }
    }
}

