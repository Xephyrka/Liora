@file:Suppress("AssignedValueIsNeverRead")

package com.xephyrka.liora.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.TaskList
import com.xephyrka.liora.data.model.RecurrenceUnit
import com.xephyrka.liora.ui.components.ListPickerDialog
import com.xephyrka.liora.ui.components.RecurrencePickerDialog
import com.xephyrka.liora.ui.theme.LioraTheme
import com.xephyrka.liora.viewmodel.TaskViewModel
import com.xephyrka.liora.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Screen that handles both creation of new tasks/notes and editing of existing ones.
 * It manages complex form state, including task details, list categorization,
 * scheduling, and recurrence rules.
 */
@Composable
fun NewTaskScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = viewModel(), 
    listId: Int,                            
    taskId: Int,                            
    initialType: ItemType = ItemType.TASK,  
) {
    // Collect reactive state from ViewModel
    val tasks by viewModel.tasks.collectAsState()
    val taskLists by viewModel.taskLists.collectAsState()
    val allSubTasks by viewModel.subtasks.collectAsState()
    val existingTask = tasks.find { it.id == taskId }

    NewTaskScreenContent(
        navController = navController,
        listId = listId,
        taskId = taskId,
        initialType = initialType,
        existingTask = existingTask,
        taskLists = taskLists,
        allSubTasks = allSubTasks,
        onAddTask = { title, content, lId, type, reminder, subs, isRec, recInt, recUnit, recDays, recEndD, recEndO ->
            viewModel.addTask(title, content, lId, type, reminder, subs, isRec, recInt, recUnit, recDays, recEndD, recEndO)
        },
        onUpdateTask = { task, subs ->
            viewModel.updateTask(task, subs)
        },
        onDeleteTask = { task ->
            viewModel.deleteTask(task)
        }
    ) { name ->
        viewModel.addTaskList(name)
    }
}

/**
 * The core UI implementation for the New/Edit Task screen.
 * Orchestrates form validation, user input collection, and various dialogs 
 * for selecting lists, reminders, and recurrence settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskScreenContent(
    navController: NavHostController,
    listId: Int,
    taskId: Int,
    initialType: ItemType = ItemType.TASK,
    existingTask: Task? = null,
    taskLists: List<TaskList> = emptyList(),
    allSubTasks: List<SubTask> = emptyList(),
    onAddTask: (String, String, Int, ItemType, Long?, List<SubTask>, Boolean, Int, RecurrenceUnit, String?, Long?, Int?) -> Unit,
    onUpdateTask: (Task, List<SubTask>) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTaskList: (String) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = taskId != -1

    // Core form state variables
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedListId by remember { mutableIntStateOf(if (taskId == -1) -1 else listId) }
    var content by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<Long?>(null) }

    // Recurrence logic state variables
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceInterval by remember { mutableIntStateOf(1) }
    var recurrenceUnit by remember { mutableStateOf(RecurrenceUnit.DAY) }
    var recurrenceDaysOfWeek by remember { mutableStateOf<String?>(null) }
    var recurrenceEndDate by remember { mutableStateOf<Long?>(null) }
    var recurrenceEndOccurrences by remember { mutableStateOf<Int?>(null) }

    // Subtask management
    val subTasks = remember { mutableStateListOf<SubTask>() }
    var dataLoaded by remember { mutableStateOf(value = false) }

    // Dialog and picker visibility toggles
    var showReminderDatePicker by remember { mutableStateOf(value = false) }
    var showReminderTimeInput by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var showListPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Auto-select newly created list
    var lastCreatedListName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(taskLists, lastCreatedListName) {
        if (lastCreatedListName != null) {
            val newList = taskLists.find { it.name == lastCreatedListName }
            if (newList != null) {
                selectedListId = newList.id
                lastCreatedListName = null
            }
        }
    }

    // Populate form fields when in edit mode
    LaunchedEffect(existingTask, allSubTasks) {
        if ((isEditMode && !dataLoaded) && (existingTask != null)) {
            title = existingTask.title
            content = existingTask.content
            selectedType = existingTask.itemType
            selectedListId = existingTask.listId
            reminderTime = existingTask.reminderTime
            
            isRecurring = existingTask.isRecurring
            recurrenceInterval = existingTask.recurrenceInterval
            recurrenceUnit = existingTask.recurrenceUnit
            recurrenceDaysOfWeek = existingTask.recurrenceDaysOfWeek
            recurrenceEndDate = existingTask.recurrenceEndDate
            recurrenceEndOccurrences = existingTask.recurrenceEndOccurrences

            val associatedSubTasks = allSubTasks.filter { it.taskId == taskId }
            if (associatedSubTasks.isNotEmpty() || allSubTasks.isNotEmpty()) {
                subTasks.clear()
                subTasks.addAll(associatedSubTasks)
                dataLoaded = true
            }
        }
    }

    // Dialog for list creation
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
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
                            lastCreatedListName = newListName
                            onAddTaskList(newListName)
                            showCreateListDialog = false
                            newListName = ""
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }, shape = MaterialTheme.shapes.medium) { Text("Cancel") }
            }
        )
    }

    // Dialog for selecting an existing list
    if (showListPicker) {
        ListPickerDialog(
            onDismiss = { showListPicker = false },
            lists = taskLists,
            selectedListId = selectedListId,
            onListSelected = {
                selectedListId = it
                showListPicker = false
            }
        )
    }

    // Dialog for confirming task deletion
    if (showDeleteConfirmation && existingTask != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${if (selectedType == ItemType.NOTE) "Note" else "Task"}?") },
            text = { Text("Are you sure you want to delete this ${if (selectedType == ItemType.NOTE) "note" else "task"}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(existingTask)
                        showDeleteConfirmation = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }, shape = MaterialTheme.shapes.medium) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditMode -> "Edit ${if (selectedType == ItemType.NOTE) "Note" else "Task"}"
                            selectedType == ItemType.NOTE -> "New Note"
                            else -> "New Task"
                        }
                    )
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            OutlinedTextField(
                value = if (selectedListId != -1) (taskLists.find { it.id == selectedListId }?.name ?: "Select List") else "None (Inbox)",
                onValueChange = {},
                readOnly = true,
                label = { Text("List") },
                trailingIcon = { 
                    IconButton(onClick = { showCreateListDialog = true }) {
                        Icon(painterResource(R.drawable.list_add), contentDescription = "Add List")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable {
                        if (taskLists.isEmpty() || (taskLists.size == 1 && taskLists[0].name == "All Tasks")) {
                            showCreateListDialog = true
                        } else {
                            showListPicker = true
                        }
                    },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (selectedType == ItemType.NOTE) "Note Title" else "Task title*") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        if (selectedType == ItemType.NOTE) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Note content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                placeholder = { Text("Write your thoughts...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { showReminderDatePicker = true }
            ) {
                OutlinedTextField(
                    value = reminderTime?.let { formatDate(it, DateFormat.is24HourFormat(context)) } ?: "Not set",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Time") },
                    leadingIcon = { Icon(painterResource(id = R.drawable.clock_checked), null) },
                    trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Subtask input state
            var currentSubTask by remember { mutableStateOf("") } 

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Steps (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                subTasks.forEachIndexed { index, sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sub.isCompleted,
                            onCheckedChange = { isChecked ->
                                subTasks[index] = sub.copy(isCompleted = isChecked)
                            }
                        )
                        OutlinedTextField(
                            value = sub.title,
                            onValueChange = { newValue ->
                                subTasks[index] = sub.copy(title = newValue)
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Step ${index + 1}") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            trailingIcon = {
                                IconButton(onClick = { subTasks.removeAt(index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Step"
                                    )
                                }
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = currentSubTask,
                    onValueChange = { currentSubTask = it },
                    label = { Text("Add a step...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (currentSubTask.isNotBlank()) {
                                subTasks.add(
                                    SubTask(
                                        taskId = taskId,
                                        title = currentSubTask,
                                        isCompleted = false
                                    )
                                )
                                currentSubTask = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if ((isEditMode) && (existingTask != null)) {
                            onUpdateTask(
                                existingTask.copy(
                                    title = title,
                                    content = content,
                                    listId = selectedListId,
                                    itemType = selectedType,
                                    reminderTime = reminderTime,
                                    isRecurring = isRecurring,
                                    recurrenceInterval = recurrenceInterval,
                                    recurrenceUnit = recurrenceUnit,
                                    recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                                    recurrenceEndDate = recurrenceEndDate,
                                    recurrenceEndOccurrences = recurrenceEndOccurrences
                                ),
                                subTasks.toList()
                            )
                        } else {
                            onAddTask(
                                title,
                                content,
                                selectedListId,
                                selectedType,
                                reminderTime,
                                subTasks.toList(),
                                isRecurring,
                                recurrenceInterval,
                                recurrenceUnit,
                                recurrenceDaysOfWeek,
                                recurrenceEndDate,
                                recurrenceEndOccurrences
                            )
                        }
                        navController.navigateUp()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Confirm")
                }
            }
        }
    }

    // Reminder date selection dialog
    if (showReminderDatePicker) {
        var tempReminderTime by remember { mutableStateOf(reminderTime) }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = tempReminderTime ?: System.currentTimeMillis(),
            yearRange = Calendar.getInstance().get(Calendar.YEAR)..(Calendar.getInstance().get(Calendar.YEAR) + 10)
        )
        
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            reminderTime = null
                            showReminderDatePicker = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove")
                    }
                    Row {
                        TextButton(onClick = { showReminderDatePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            
                            val timeCal = Calendar.getInstance()
                            timeCal.timeInMillis = tempReminderTime ?: System.currentTimeMillis()
                            
                            cal.set(Calendar.HOUR_OF_DAY, timeCal[Calendar.HOUR_OF_DAY])
                            cal.set(Calendar.MINUTE, timeCal[Calendar.MINUTE])
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            
                            reminderTime = cal.timeInMillis
                            showReminderDatePicker = false
                        }) {
                            Text("Done")
                        }
                    }
                }
            },
            dismissButton = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = null,
                    headline = null
                )
                
                HorizontalDivider()
                
                ListItem(
                    headlineContent = { 
                        Text(
                            tempReminderTime?.let {
                                "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                            } ?: "Set Time"
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier
                        .clickable { showReminderTimeInput = true },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
                
                HorizontalDivider()
                
                ListItem(
                    headlineContent = {
                        Text(
                            if (isRecurring) {
                                val repeatText = when (recurrenceUnit) {
                                    RecurrenceUnit.DAY -> if (recurrenceInterval == 1) "Daily" else "Every $recurrenceInterval days"
                                    RecurrenceUnit.WEEK -> if (recurrenceInterval == 1) "Weekly" else "Every $recurrenceInterval weeks"
                                    RecurrenceUnit.MONTH -> if (recurrenceInterval == 1) "Monthly" else "Every $recurrenceInterval months"
                                }
                                "Repeats: $repeatText"
                            } else "Set Repeat"
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Refresh, null) },
                    modifier = Modifier
                        .clickable { showRecurrencePicker = true },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
                HorizontalDivider()
            }
        }

        // Time input dialog for reminder (moved inside to use tempReminderTime)
        if (showReminderTimeInput) {
            val cal = Calendar.getInstance().apply{
                timeInMillis = tempReminderTime ?: System.currentTimeMillis()
            }
            val timeInputState = rememberTimePickerState(
                initialHour = cal[Calendar.HOUR_OF_DAY],
                initialMinute = cal[Calendar.MINUTE],
                is24Hour = DateFormat.is24HourFormat(context)
            )
            AlertDialog(
                onDismissRequest = { showReminderTimeInput = false },
                confirmButton = {
                    TextButton(onClick = {
                        val finalCal = Calendar.getInstance()
                        val dateMillis = tempReminderTime ?: System.currentTimeMillis()
                        finalCal.timeInMillis = dateMillis
                        
                        finalCal.set(Calendar.HOUR_OF_DAY, timeInputState.hour)
                        finalCal.set(Calendar.MINUTE, timeInputState.minute)
                        finalCal.set(Calendar.SECOND, 0)
                        finalCal.set(Calendar.MILLISECOND, 0)

                        tempReminderTime = finalCal.timeInMillis
                        showReminderTimeInput = false
                    }) { Text("Set Time") }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderTimeInput = false }) { Text("Cancel") }
                },
                title = { Text("Start Time") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimeInput(state = timeInputState)
                    }
                }
            )
        }
    }

    if (showRecurrencePicker) {
        RecurrencePickerDialog(
            onDismiss = { showRecurrencePicker = false },
            onConfirm = { interval, unit, daysOfWeek, _, _ ->
                if (interval == 0) {
                    isRecurring = false
                } else {
                    isRecurring = true
                    recurrenceInterval = interval
                    recurrenceUnit = unit
                    recurrenceDaysOfWeek = daysOfWeek
                }
                showRecurrencePicker = false
            },
            initialInterval = recurrenceInterval,
            initialUnit = recurrenceUnit,
            initialDaysOfWeek = recurrenceDaysOfWeek
        )
    }
}

/**
 * Utility to format timestamp into human-readable date/time string,
 * considering system 12/24 hour settings.
 */
private fun formatDate(timeInMillis: Long?, is24Hour: Boolean): String {
    if (timeInMillis == null) return ""
    val calendar = Calendar.getInstance().apply {
        this.timeInMillis = timeInMillis
    }
    val pattern = if (is24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(calendar.time)
}

@Preview(showBackground = true)
@Composable
fun NewTaskScreenPreviewNote() {
    val navController = rememberNavController()
    LioraTheme {
        NewTaskScreenContent(
            navController = navController,
            listId = 1,
            taskId = -1,
            initialType = ItemType.NOTE,
            taskLists = listOf(
                TaskList(id = 1, name = "Daily"),
                TaskList(id = 2, name = "Work"),
            ),
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onUpdateTask = { _, _ -> },
            onDeleteTask = { _ -> },
            onAddTaskList = { _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NewTaskScreenPreviewTask() {
    val navController = rememberNavController()
    LioraTheme {
        NewTaskScreenContent(
            navController = navController,
            listId = 1,
            taskId = -1,
            initialType = ItemType.TASK,
            taskLists = listOf(
                TaskList(id = 1, name = "Daily"),
                TaskList(id = 2, name = "Work"),
            ),
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onUpdateTask = { _, _ -> },
            onDeleteTask = { _ -> },
            onAddTaskList = { _ -> }
        )
    }
}
