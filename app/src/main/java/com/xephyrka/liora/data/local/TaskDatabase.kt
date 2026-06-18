package com.xephyrka.liora.data.local

import android.content.Context
import androidx.room.*
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.TaskList

/**
 * The main Room database class for the Liora application.
 * Manages the persistence of tasks, task lists, and subtasks.
 */
@Database(
    entities = [Task::class, TaskList::class, SubTask::class],
    version = 1,
    exportSchema = false,
)
abstract class TaskDatabase : RoomDatabase() {
    /** Provides access to the Data Access Object (DAO) for database operations. */
    abstract fun taskDao(): TaskDao

    companion object {
        /** The singleton instance of the database to ensure only one connection is open. */
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        /** 
         * Returns the singleton instance of the [TaskDatabase].
         * If the instance doesn't exist, it initializes it using a synchronized block for thread safety.
         */
        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "liora_database"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
