package com.xephyrka.liora.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.xephyrka.liora.AlarmScheduler
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.repository.TaskRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Local unit tests for the TaskViewModel.
 * Verifies the business logic of task state management, deletion confirmations, and alarm coordination.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    /** Rule to force background tasks to run synchronously for testing. */
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    /** Test dispatcher used to control coroutine execution during tests. */
    private val testDispatcher = UnconfinedTestDispatcher()

    /** The instance under test. */
    private lateinit var viewModel: TaskViewModel
    /** Mocked repository for data access. */
    private val repository: TaskRepository = mockk(relaxed = true)
    /** Mocked scheduler for alarm operations. */
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    /** Mocked application context. */
    private val application: Application = mockk(relaxed = true)

    /** 
     * Initializes the testing environment, including setting the Main dispatcher 
     * and providing initial mocked flows for the ViewModel.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { repository.taskLists } returns flowOf(emptyList())
        every { repository.tasks } returns flowOf(emptyList())
        every { repository.subtasks } returns flowOf(emptyList())

        viewModel = TaskViewModel(application, repository, alarmScheduler)
    }

    /** 
     * Resets the Main dispatcher after each test to avoid leaking test state.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 
     * Verifies that toggling an uncompleted task correctly triggers the deletion confirmation state.
     */
    @Test
    fun `onTaskCompletionToggled when not completed should set taskToDelete`() {
        val task = Task(id = 1, title = "Test Task", isCompleted = false, listId = 1, itemType = ItemType.TASK)
        
        viewModel.onTaskCompletionToggled(task)
        
        assertEquals(task, viewModel.taskToDelete.value)
    }

    /** 
     * Verifies that toggling an already completed task simply marks it as uncompleted in the repository.
     */
    @Test
    fun `onTaskCompletionToggled when already completed should update repository to false`() {
        val task = Task(id = 1, title = "Test Task", isCompleted = true, listId = 1, itemType = ItemType.TASK)
        
        viewModel.onTaskCompletionToggled(task)
        
        coVerify { repository.updateTask(match { !it.isCompleted }) }
        assertNull(viewModel.taskToDelete.value)
    }

    /** 
     * Verifies that confirming task deletion correctly invokes repository and alarm scheduler methods.
     */
    @Test
    fun `confirmTaskDeletion should delete task and cancel alarm`() {
        val task = Task(id = 1, title = "Test Task", isCompleted = false, listId = 1, itemType = ItemType.TASK)
        viewModel.onTaskCompletionToggled(task)
        
        viewModel.confirmTaskDeletion()
        
        coVerify { repository.deleteTask(task) }
        coVerify { alarmScheduler.cancel(task) }
        assertNull(viewModel.taskToDelete.value)
    }

    /** 
     * Verifies that canceling task deletion clears the internal pending-deletion state.
     */
    @Test
    fun `cancelTaskDeletion should clear taskToDelete`() {
        val task = Task(id = 1, title = "Test Task", isCompleted = false, listId = 1, itemType = ItemType.TASK)
        viewModel.onTaskCompletionToggled(task)
        
        viewModel.cancelTaskDeletion()
        
        assertNull(viewModel.taskToDelete.value)
    }
}
