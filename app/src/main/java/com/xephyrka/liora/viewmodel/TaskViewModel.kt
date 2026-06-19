package com.xephyrka.liora.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xephyrka.liora.AlarmScheduler
import com.xephyrka.liora.data.model.BackupData
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.TaskList
import com.xephyrka.liora.data.repository.TaskRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel for managing task-related data and operations. It provides StateFlows for the UI to observe and methods for adding, updating, and deleting tasks and lists
class TaskViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TaskRepository = TaskRepository(application),
    private val alarmScheduler: AlarmScheduler = AlarmScheduler(application)
) : AndroidViewModel(application) {
    private val gson = Gson() // JSON parser for data import and export

    // StateFlow containing the list of all available task lists
    val taskLists: StateFlow<List<TaskList>> = repository.taskLists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedListId = MutableStateFlow<Int?>(null) // Currently selected list ID for filtering tasks
    private val _searchQuery = MutableStateFlow("") // Current search query for filtering tasks by title or content
    fun filterByList(listId: Int?) // Updates the selected list filter
    {
        _selectedListId.value = listId
    }


    fun setSearchQuery(query: String) // Updates the current search query
    {
        _searchQuery.value = query
    }

    // StateFlow containing the filtered list of tasks. Filtered by both the selected list and the current search query
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = combine(
        _selectedListId.flatMapLatest { listId ->
            if (listId == null) repository.tasks else repository.getTasksForList(listId)
        },
        _searchQuery
    ) { tasks, query ->
        if (query.isBlank()) {
            tasks
        } else {
            tasks.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // StateFlow containing all subtasks
    val subtasks: StateFlow<List<SubTask>> = repository.subtasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _taskToDelete = MutableStateFlow<Task?>(null) // Internal state for the task currently being considered for deletion or completion

    val taskToDelete = _taskToDelete.asStateFlow() // Public access to the task pending deletion


    fun onTaskCompletionToggled(task: Task) // Handles toggling the completion status of a task. If the task is being marked as complete, it triggers a confirmation state
    {
        if (task.isCompleted) {
            viewModelScope.launch {
                repository.updateTask(task.copy(isCompleted = false))
            }
        } else {
            if (task.itemType == ItemType.NOTE) {
                viewModelScope.launch {
                    repository.updateTask(task.copy(isCompleted = true))
                    _taskToDelete.value = null
                }
            } else {
                _taskToDelete.value = task
            }
        }
    }

    fun confirmTaskDeletion() // Permanently deletes the task currently stored in [_taskToDelete]. Also cancels any scheduled alarms for this task
    {
        _taskToDelete.value?.let { task ->
            viewModelScope.launch {
                repository.deleteTask(task)
                alarmScheduler.cancel(task)
                _taskToDelete.value = null
            }
        }
    }


    fun cancelTaskDeletion()// Resets the deletion confirmation state without deleting the task
    {
        _taskToDelete.value = null
    }

    fun onSubTaskToggled(subtask: SubTask) // Toggles the completion status of a subtask
    {
        viewModelScope.launch {
            repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
        }
    }


    suspend fun getTaskById(id: Int): Task? // Retrieves a specific task by its unique identifier
    {
        return repository.getTaskById(id)
    }

    // Adds a new task to the database and schedules a reminder if applicable. Handles both regular tasks and recurring tasks
    fun addTask(
        title: String,
        content: String = "",
        listId: Int,
        itemType: ItemType,
        reminder: Long? = null,
        subtasks: List<SubTask> = emptyList(),
        isRecurring: Boolean = false,
        recurrenceInterval: Int = 1,
        recurrenceUnit: com.xephyrka.liora.data.model.RecurrenceUnit = com.xephyrka.liora.data.model.RecurrenceUnit.DAY,
        recurrenceDaysOfWeek: String? = null,
        recurrenceEndDate: Long? = null,
        recurrenceEndOccurrences: Int? = null
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                content = if (itemType == ItemType.NOTE) content else "",
                listId = listId,
                itemType = itemType,
                reminderTime = if (itemType == ItemType.NOTE) null else reminder,
                isCompleted = false,
                isRecurring = isRecurring,
                recurrenceInterval = recurrenceInterval,
                recurrenceUnit = recurrenceUnit,
                recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                recurrenceEndDate = recurrenceEndDate,
                recurrenceEndOccurrences = recurrenceEndOccurrences
            )
            val taskId = repository.insertTask(task)
            val newTask = task.copy(id = taskId.toInt())
            if (itemType != ItemType.NOTE) {
                alarmScheduler.schedule(newTask)
            }
            subtasks.forEach {
                repository.insertSubtask(it.copy(taskId = taskId.toInt()))
            }
        }
    }


    fun updateTask(task: Task, subtasks: List<SubTask>? = null) // Updates an existing task and its associated subtasks. Reschedules or cancels reminders based on the updated task type and time
    {
        viewModelScope.launch {
            val taskToUpdate = if (task.itemType != ItemType.NOTE) task.copy(content = "") else task
            repository.updateTask(taskToUpdate)
            if (taskToUpdate.itemType != ItemType.NOTE) {
                alarmScheduler.schedule(taskToUpdate)
            } else {
                alarmScheduler.cancel(taskToUpdate)
            }
            if (subtasks != null) {
                repository.deleteSubtasksByTaskId(taskToUpdate.id)
                subtasks.forEach {
                    repository.insertSubtask(it.copy(taskId = taskToUpdate.id, id = 0))
                }
            }
        }
    }


    fun deleteTask(task: Task) // Deletes a task from the database and cancels its scheduled alarm
    {
        viewModelScope.launch {
            repository.deleteTask(task)
            alarmScheduler.cancel(task)
        }
    }


    fun addTaskList(name: String) // Adds a new custom task list
    {
        viewModelScope.launch {
            repository.insertTaskList(TaskList(name = name))
        }
    }


    fun updateTaskList(taskList: TaskList, newName: String) // Renames an existing task list
    {
        viewModelScope.launch {
            repository.insertTaskList(taskList.copy(name = newName))
        }
    }


    fun deleteTaskList(taskList: TaskList) // Deletes a task list and all tasks associated with it
    {
        viewModelScope.launch {
            repository.deleteTaskList(taskList)
        }
    }


    fun clearAllData() // Clears all data from the database, including tasks, subtasks, and lists. Cancels all currently scheduled alarms.
    {
        viewModelScope.launch {
            val currentTasks = tasks.value
            withContext(Dispatchers.IO) {
                repository.clearAllData()
            }
            currentTasks.forEach { alarmScheduler.cancel(it) }
        }
    }


    fun rescheduleAllAlarms() // Iterates through all active tasks and re-schedules reminders if they are in the future.  Useful after a device reboot or app update
    {
        viewModelScope.launch {
            tasks.value.forEach { task ->
                if (task.itemType != ItemType.NOTE && task.reminderTime != null && task.reminderTime > System.currentTimeMillis()) {
                    alarmScheduler.schedule(task)
                }
            }
        }
    }


    suspend fun getExportDataJson(): String // Generates a JSON string representing all app data for backup purposes
    {
        val (lists, tasks, subtasks) = repository.getAllDataSnapshot()
        val backupData = BackupData(lists, tasks, subtasks)
        return gson.toJson(backupData)
    }


    fun importDataFromJson(json: String) // Restores app data from a JSON string. Cancels existing alarms before overwriting the database and scheduling new ones
    {
        viewModelScope.launch {
            try {
                val backupData = gson.fromJson(json, BackupData::class.java)
                tasks.value.forEach { alarmScheduler.cancel(it) }
                
                withContext(Dispatchers.IO) {
                    repository.restoreData(
                        backupData.taskLists,
                        backupData.tasks,
                        backupData.subtasks
                    )
                }
                
                backupData.tasks.forEach { task ->
                    if (task.itemType != ItemType.NOTE && task.reminderTime != null && task.reminderTime > System.currentTimeMillis()) {
                        alarmScheduler.schedule(task)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
