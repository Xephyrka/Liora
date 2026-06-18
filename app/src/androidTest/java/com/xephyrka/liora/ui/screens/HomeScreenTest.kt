package com.xephyrka.liora.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.viewmodel.TaskViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI tests for the HomeScreen Composable.
 * Uses MockK to isolate UI behavior from the underlying database and business logic.
 */
class HomeScreenTest {

    /** Rule for launching and testing Compose-based UI components. */
    @get:Rule
    val composeTestRule = createComposeRule()

    /** Mocked ViewModel to provide controlled state and capture interactions. */
    private val viewModel: TaskViewModel = mockk(relaxed = true)
    /** Mocked Navigation controller to avoid real navigation during tests. */
    private val navController: NavHostController = mockk(relaxed = true)

    /** 
     * Verifies that clicking a task's completion checkbox correctly triggers 
     * the visibility of the confirmation dialog.
     */
    @Test
    fun clickingComplete_showsConfirmationDialog() {
        /** Sample task data for the test. */
        val task = Task(id = 1, title = "Test Task", isCompleted = false, listId = 1, itemType = ItemType.TASK)
        /** Mocked flow for tasks. */
        val tasksFlow = MutableStateFlow(listOf(task))
        /** Mocked flow for the task currently being deleted. */
        val taskToDeleteFlow = MutableStateFlow<Task?>(null)

        every { viewModel.tasks } returns tasksFlow
        every { viewModel.taskLists } returns MutableStateFlow(emptyList())
        every { viewModel.subtasks } returns MutableStateFlow(emptyList())
        every { viewModel.taskToDelete } returns taskToDeleteFlow

        every { viewModel.onTaskCompletionToggled(task) } answers {
            taskToDeleteFlow.value = task
        }

        composeTestRule.setContent {
            HomeScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithTag("TaskCheckbox_1").performClick()

        verify { viewModel.onTaskCompletionToggled(task) }
        composeTestRule.onNodeWithText("Confirm Completion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to mark this task as completed? It will be deleted.").assertIsDisplayed()
    }

    /** 
     * Verifies that confirming task completion in the dialog calls the 
     * appropriate confirmation method on the ViewModel.
     */
    @Test
    fun confirmDeletion_callsViewModelConfirm() {
        /** Sample task data for the test. */
        val task = Task(id = 1, title = "Test Task", isCompleted = false, listId = 1, itemType = ItemType.TASK)
        /** Mocked flow for tasks. */
        val tasksFlow = MutableStateFlow(listOf(task))
        /** Mocked flow initialized to show the confirmation dialog immediately. */
        val taskToDeleteFlow = MutableStateFlow<Task?>(task)

        every { viewModel.tasks } returns tasksFlow
        every { viewModel.taskLists } returns MutableStateFlow(emptyList())
        every { viewModel.subtasks } returns MutableStateFlow(emptyList())
        every { viewModel.taskToDelete } returns taskToDeleteFlow

        composeTestRule.setContent {
            HomeScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithTag("ConfirmCompleteButton").performClick()

        verify { viewModel.confirmTaskDeletion() }
    }
}
