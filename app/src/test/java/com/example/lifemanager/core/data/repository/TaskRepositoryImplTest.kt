package com.example.lifemanager.core.data.repository

import com.example.lifemanager.core.data.local.TaskDao
import com.example.lifemanager.core.data.local.entity.TaskEntity
import com.example.lifemanager.core.domain.model.Task
import com.example.lifemanager.core.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskRepositoryImplTest {

    private lateinit var repository: TaskRepositoryImpl
    private lateinit var taskDao: TaskDao

    private val testEntity = TaskEntity(
        id = 1,
        title = "Test Task",
        description = "Test Description",
        scheduledDate = 1700000000000L,
        scheduledTimeMinutes = 600,
        plannedDurationMinutes = 60,
        color = 0xFF2196F3.toInt(),
        status = TaskStatus.PLANNED
    )

    @Before
    fun setup() {
        taskDao = mockk(relaxed = true)
        repository = TaskRepositoryImpl(taskDao)
    }

    @Test
    fun `getTasksForDate should return mapped tasks`() = runTest {
        coEvery { taskDao.getTasksByDate(any()) } returns flowOf(listOf(testEntity))

        val result = repository.getTasksForDate(1700000000000L).first()

        assertEquals(1, result.size)
        assertEquals("Test Task", result[0].title)
        assertEquals(TaskStatus.PLANNED, result[0].status)
    }

    @Test
    fun `getTaskById should return mapped task`() = runTest {
        coEvery { taskDao.getTaskById(1) } returns testEntity

        val result = repository.getTaskById(1)

        assertEquals("Test Task", result?.title)
        assertEquals(1, result?.id)
    }

    @Test
    fun `getTaskById should return null for non-existent task`() = runTest {
        coEvery { taskDao.getTaskById(999) } returns null

        val result = repository.getTaskById(999)

        assertEquals(null, result)
    }

    @Test
    fun `createTask should insert and return new task`() = runTest {
        coEvery { taskDao.insertTask(any()) } returns 1L
        coEvery { taskDao.getTaskById(1) } returns testEntity

        val result = repository.createTask(
            title = "Test Task",
            durationMinutes = 60,
            scheduledDate = 1700000000000L,
            scheduledTimeMinutes = 600,
            color = 0xFF2196F3.toInt()
        )

        assertEquals("Test Task", result.title)
        coVerify { taskDao.insertTask(any()) }
    }

    @Test
    fun `startTask should update task status to IN_PROGRESS`() = runTest {
        val task = Task(
            id = 1,
            title = "Test Task",
            description = "",
            scheduledDate = 1700000000000L,
            scheduledTimeMinutes = 600,
            plannedDurationMinutes = 60,
            color = 0xFF2196F3.toInt(),
            status = TaskStatus.PLANNED
        )

        val result = repository.startTask(task)

        assertEquals(TaskStatus.IN_PROGRESS, result.status)
        coVerify { taskDao.updateTask(any()) }
    }

    @Test
    fun `completeTask should update task with work log`() = runTest {
        val task = Task(
            id = 1,
            title = "Test Task",
            description = "",
            scheduledDate = 1700000000000L,
            scheduledTimeMinutes = 600,
            plannedDurationMinutes = 60,
            color = 0xFF2196F3.toInt(),
            status = TaskStatus.IN_PROGRESS
        )

        val result = repository.completeTask(task, "Work completed")

        assertEquals(TaskStatus.COMPLETED, result.status)
        assertEquals("Work completed", result.workLog)
        coVerify { taskDao.updateTask(any()) }
    }

    @Test
    fun `deleteTask should call dao delete`() = runTest {
        val task = Task(
            id = 1,
            title = "Test Task",
            description = "",
            scheduledDate = 1700000000000L,
            scheduledTimeMinutes = 600,
            plannedDurationMinutes = 60,
            color = 0xFF2196F3.toInt(),
            status = TaskStatus.PLANNED
        )

        repository.deleteTask(task)

        coVerify { taskDao.deleteTask(any()) }
    }
}

