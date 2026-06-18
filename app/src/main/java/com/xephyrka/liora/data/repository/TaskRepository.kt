package com.xephyrka.liora.data.repository

import android.content.Context
import com.xephyrka.liora.data.local.TaskDatabase
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.TaskList
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to the database.
 * Provides a clean API for the rest of the application (mainly ViewModels) to interact with task data.
 */
class TaskRepository(context: Context) {
    /** Internal reference to the Room database. */
    private val database = TaskDatabase.getDatabase(context)
    /** Internal reference to the Data Access Object. */
    private val taskDao = database.taskDao()

    /** Flow emitting the list of all task lists. */
    val taskLists: Flow<List<TaskList>> = taskDao.getAllTaskLists()
    /** Flow emitting the list of all tasks. */
    val tasks: Flow<List<Task>> = taskDao.getAllTasks()
    /** Flow emitting the list of all subtasks. */
    val subtasks: Flow<List<SubTask>> = taskDao.getAllSubtasks()

    /** Returns a Flow of tasks filtered by a specific list ID. */
    fun getTasksForList(listId: Int): Flow<List<Task>> {
        return taskDao.getTasksForList(listId)
    }

    /** Retrieves a task by its unique ID. */
    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    /** Persists a new or updated task list. */
    suspend fun insertTaskList(taskList: TaskList) {
        taskDao.insertList(taskList)
    }

    /** Deletes a task list from the database. */
    suspend fun deleteTaskList(taskList: TaskList){
        taskDao.deleteTaskList(taskList)
    }

    /** 
     * Inserts a new task and returns its generated ID.
     * Uses OnConflictStrategy.REPLACE internally via the DAO.
     */
    suspend fun insertTask(task: Task): Long {
        return taskDao.insert(task)
    }

    /** Updates an existing task's data. */
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    /** Removes a task from the database. */
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    /** Adds a new subtask associated with a task. */
    suspend fun insertSubtask(subtask: SubTask) {
        taskDao.insertSubtask(subtask)
    }

    /** Updates an existing subtask (e.g., completion status). */
    suspend fun updateSubtask(subtask: SubTask) {
        taskDao.updateSubtask(subtask)
    }

    /** Deletes all subtasks for a specific parent task. */
    suspend fun deleteSubtasksByTaskId(taskId: Int) {
        taskDao.deleteSubtasksByTaskId(taskId)
    }

    /** Wipes all data from all tables in the database. */
    fun clearAllData() {
        database.clearAllTables()
    }

    /** 
     * Retrieves a full snapshot of the database for backup purposes.
     * Returns a Triple containing lists of all TaskLists, Tasks, and Subtasks.
     */
    suspend fun getAllDataSnapshot(): Triple<List<TaskList>, List<Task>, List<SubTask>> {
        return Triple(
            taskDao.getAllTaskListsSnapshot(),
            taskDao.getAllTasksSnapshot(),
            taskDao.getAllSubtasksSnapshot()
        )
    }

    /** 
     * Wipes current data and restores the database state from the provided lists.
     * Performed as a single transaction via the DAO.
     */
    suspend fun restoreData(lists: List<TaskList>, tasks: List<Task>, subtasks: List<SubTask>) {
        taskDao.clearAndRestore(lists, tasks, subtasks)
    }
}
