package com.xephyrka.liora.data.local

import androidx.room.*
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.TaskList
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for interacting with the Liora SQLite database.
 * Provides methods for CRUD operations on Tasks, TaskLists, and Subtasks.
 */
@Dao
@JvmSuppressWildcards
interface TaskDao {
    /** 
     * Observes all tasks in the database. 
     * Returns a Flow that emits a new list whenever any task is modified.
     */
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    /** Retrieves a one-time snapshot of all tasks currently in the database. */
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSnapshot(): List<Task>

    /** 
     * Observes tasks belonging to a specific list.
     * @param listId The ID of the parent list.
     */
    @Query("SELECT * FROM tasks WHERE listId = :listId")
    fun getTasksForList(listId: Int): Flow<List<Task>>

    /** Retrieves a specific task by its unique ID. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    /** Inserts a new task or replaces an existing one if the ID conflicts. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    /** Updates the fields of an existing task. */
    @Update
    suspend fun updateTask(task: Task): Int

    /** Deletes a specific task from the database. */
    @Delete
    suspend fun deleteTask(task: Task): Int

    /** Observes all user-defined task lists. */
    @Query("SELECT * FROM task_lists")
    fun getAllTaskLists(): Flow<List<TaskList>>

    /** Retrieves a one-time snapshot of all task lists. */
    @Query("SELECT * FROM task_lists")
    suspend fun getAllTaskListsSnapshot(): List<TaskList>

    /** Inserts a new task list or replaces an existing one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(taskList: TaskList): Long

    /** Deletes a specific task list. */
    @Delete
    suspend fun deleteTaskList(taskList: TaskList): Int

    /** Observes all subtasks across all tasks. */
    @Query("SELECT * FROM subtasks")
    fun getAllSubtasks(): Flow<List<SubTask>>

    /** Retrieves a one-time snapshot of all subtasks. */
    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksSnapshot(): List<SubTask>

    /** Inserts a new subtask. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubTask): Long

    /** Updates the status or title of an existing subtask. */
    @Update
    suspend fun updateSubtask(subtask: SubTask): Int

    /** Deletes all subtasks associated with a specific parent task. */
    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: Int): Int

    /** 
     * Performs a full database restoration within a single transaction.
     * Wipes all existing data before inserting the provided backup entities.
     */
    @Transaction
    suspend fun clearAndRestore(lists: List<TaskList>, tasks: List<Task>, subtasks: List<SubTask>) {
        deleteAllSubtasks()
        deleteAllTasks()
        deleteAllTaskLists()
        
        lists.forEach { insertList(it) }
        tasks.forEach { insert(it) }
        subtasks.forEach { insertSubtask(it) }
    }

    /** Deletes all tasks from the database. */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks(): Int

    /** Deletes all task lists from the database. */
    @Query("DELETE FROM task_lists")
    suspend fun deleteAllTaskLists(): Int

    /** Deletes all subtasks from the database. */
    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks(): Int
}
