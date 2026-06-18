package com.xephyrka.liora.data.model

/**
 * A data transfer object (DTO) used for importing and exporting application data.
 * It encapsulates all relevant entities to ensure a consistent state during backup operations.
 */
data class BackupData(
    /** All task lists currently saved in the application. */
    val taskLists: List<TaskList>,
    /** All individual tasks (both active and completed). */
    val tasks: List<Task>,
    /** All subtasks associated with the exported tasks. */
    val subtasks: List<SubTask>
)
