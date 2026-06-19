@file:Suppress("AssignedValueIsNeverRead")

package com.xephyrka.liora.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.xephyrka.liora.navigation.Screen
import com.xephyrka.liora.ui.components.TaskItem
import com.xephyrka.liora.ui.components.TaskTabs
import com.xephyrka.liora.viewmodel.TaskViewModel
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.TaskList
import com.xephyrka.liora.data.model.ItemType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import com.xephyrka.liora.ui.components.ShowcaseState
import com.xephyrka.liora.ui.theme.LioraTheme

/**
 * Screen that serves as the main dashboard for the application.
 * It observes the ViewModel to display categorized task lists and tasks, 
 * handles filtering, searching, and provides entry points for task management.
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: TaskViewModel,
    externalPadding: PaddingValues = PaddingValues(0.dp),
    showcaseState: ShowcaseState? = null
){
    // Collected state from the ViewModel to drive the UI
    val taskLists by viewModel.taskLists.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val subtasks by viewModel.subtasks.collectAsState()
    val taskToDelete by viewModel.taskToDelete.collectAsState()

    HomeScreenContent(
        taskLists = taskLists,
        tasks = tasks,
        subtasks = subtasks,
        taskToDelete = taskToDelete,
        onFilterByList = { viewModel.filterByList(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onConfirmTaskDeletion = { viewModel.confirmTaskDeletion() },
        onCancelTaskDeletion = { viewModel.cancelTaskDeletion() },
        onUpdateTaskList = { list, name -> viewModel.updateTaskList(list, name)},
        onAddTaskList = { name -> viewModel.addTaskList(name) },
        onDeleteTaskList = { list -> viewModel.deleteTaskList(list) },
        onTaskToggle = { task -> viewModel.onTaskCompletionToggled(task) },
        onEditTask = { task -> navController.navigate(Screen.NewTask.createRoute(task.listId, task.id)) },
        onDeleteTask = { task -> viewModel.deleteTask(task) },
        onSubTaskToggle = { subtask -> viewModel.onSubTaskToggled(subtask) },
        onNavigateToNewTask = { listId, type ->
            navController.navigate(Screen.NewTask.createRoute(listId, type = type))
        },
        externalPadding = externalPadding,
        showcaseState = showcaseState
    )
}

/**
 * The main layout implementation for the HomeScreen.
 * Handles the display of the top bar, tab navigation, floating action button, 
 * and orchestration of various task interaction dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    taskLists: List<TaskList>,
    tasks: List<Task>,
    subtasks: List<SubTask>,
    taskToDelete: Task?,
    onFilterByList: (Int?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConfirmTaskDeletion: () -> Unit,
    onCancelTaskDeletion: () -> Unit,
    onUpdateTaskList: (TaskList, String) -> Unit,
    onAddTaskList: (String) -> Unit,
    onDeleteTaskList: (TaskList) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onSubTaskToggle: (SubTask) -> Unit,
    onNavigateToNewTask: (Int, ItemType) -> Unit,
    externalPadding: PaddingValues,
    showcaseState: ShowcaseState? = null
) {
    // Current state for the active tab index
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog control states for editing/creating lists
    var listToEdit by remember { mutableStateOf<TaskList?>(null) }
    var showEditDialog by remember { mutableStateOf(value = false) }
    var newName by remember { mutableStateOf("") }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    // Floating action button menu states
    var isMenuExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isMenuExpanded) 45f else 0f)

    // Search bar state management
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Effect triggers to apply filters based on selected category or search queries
    LaunchedEffect(selectedTab, taskLists) {
        val listId = if (selectedTab == 0) null else taskLists.getOrNull(selectedTab - 1)?.id
        onFilterByList(listId)
    }

    LaunchedEffect(searchQuery) {
        onSearchQueryChange(searchQuery)
    }

    // Dialog for confirming task deletion
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                onCancelTaskDeletion()
            },
            title = { Text("Confirm Completion") },
            text = { Text("Are you sure you want to mark this task as completed? It will be deleted.") },
            confirmButton = {
                Button(
                    onClick = { onConfirmTaskDeletion() },
                    modifier = Modifier.testTag("ConfirmCompleteButton"),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = { onCancelTaskDeletion() }, shape = MaterialTheme.shapes.medium) { Text("Cancel") }
            }
        )
    }

    // Dialog for editing an existing task list name
    if ((showEditDialog) && (listToEdit != null)) {
        AlertDialog(
            onDismissRequest = { if (showEditDialog) showEditDialog = false },
            title = { Text("Edit List Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTaskList(listToEdit!!, newName)
                        showEditDialog = false
                    },
                    shape = MaterialTheme.shapes.medium
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, shape = MaterialTheme.shapes.medium) { Text("Cancel") }
            }
        )
    }

    // Dialog for creating a new task list
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateListDialog = false
            },
            title = { Text("Create New List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            onAddTaskList(newListName)
                            showCreateListDialog = false
                            newListName = ""
                        }
                    },
                    shape = MaterialTheme.shapes.medium) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }, shape = MaterialTheme.shapes.medium) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search tasks...") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        isSearchActive = false
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Close Search")
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            Text(
                                text = "Liora",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .padding(bottom = externalPadding.calculateBottomPadding() + 16.dp)
                        .onGloballyPositioned { coords ->
                            showcaseState?.updateTargetCoordinates("add_target", coords)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add task",
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(rotation)
                    )
                }

                // Menu to choose between creating a task or a note
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    val currentListId = if (selectedTab == 0) {
                        taskLists.firstOrNull()?.id ?: 1
                    } else {
                        taskLists.getOrNull(selectedTab - 1)?.id ?: (taskLists.firstOrNull()?.id ?: 1)
                    }

                    DropdownMenuItem(
                        text = { Text("Note") },
                        onClick = {
                            isMenuExpanded = false
                            onNavigateToNewTask(currentListId, ItemType.NOTE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Task") },
                        onClick = {
                            isMenuExpanded = false
                            onNavigateToNewTask(currentListId, ItemType.TASK)
                        }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(1f)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            TaskTabs(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    if (newTab == taskLists.size + 1) {
                        newListName = ""
                        showCreateListDialog = true
                    } else {
                        selectedTab = newTab
                    }
                },
                taskLists = taskLists,
                onEditList = { list ->
                    listToEdit = list
                    newName = list.name
                    showEditDialog = true
                },
                onDeleteList = { list ->
                    onDeleteTaskList(list)
                    selectedTab = 0 
                },
                showcaseState = showcaseState
            )

            TaskListContent(
                tasks = tasks,
                subtasks = subtasks,
                onTaskToggle = onTaskToggle,
                onEditTask = onEditTask,
                onDeleteTask = onDeleteTask,
                onSubTaskToggle = onSubTaskToggle,
                contentPadding = PaddingValues(
                    bottom = externalPadding.calculateBottomPadding() + 16.dp,
                    top = 8.dp
                ),
                externalPadding = externalPadding
            )
        }
    }
}

/**
 * Renders the primary list of tasks using a scrollable LazyColumn.
 * Handles empty list states and dispatches user actions back to parent components.
 */
@Composable
fun TaskListContent(
    tasks: List<Task>,
    subtasks: List<SubTask>,
    onTaskToggle: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onSubTaskToggle: (SubTask) -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    externalPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (tasks.isEmpty()) {
            Text(
                text = "No tasks found",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = externalPadding.calculateBottomPadding()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = contentPadding
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        subtasks = subtasks.filter { it.taskId == task.id },
                        onToggle = { onTaskToggle(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onSubTaskToggle = { onSubTaskToggle(it) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * Preview Composable to assist in UI development.
 */
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LioraTheme {
        HomeScreenContent(
            taskLists = listOf(TaskList(id = 1, name = "Work"), TaskList(id = 3, name = "lugubrious")),
            tasks = listOf(
                Task(id = 1, title = "Go to work", listId = 1, itemType = ItemType.TASK, reminderTime = 11000000, isCompleted = false),
                Task(id = 2, title = "This is another task in your day", listId = 1, itemType = ItemType.TASK, reminderTime = 15000000, isCompleted = false),
            ),
            subtasks = emptyList(),
            taskToDelete = null,
            onFilterByList = {},
            onSearchQueryChange = {},
            onConfirmTaskDeletion = {},
            onCancelTaskDeletion = {},
            onUpdateTaskList = {_, _ ->},
            onAddTaskList = {},
            onDeleteTaskList = {},
            onTaskToggle = {},
            onEditTask = {_ ->},
            onDeleteTask = {},
            onSubTaskToggle = {},
            onNavigateToNewTask = {_, _ ->},
            externalPadding = PaddingValues(0.dp)
        )
    }
}
