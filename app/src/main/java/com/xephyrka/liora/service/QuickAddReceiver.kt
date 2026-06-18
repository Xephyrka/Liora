package com.xephyrka.liora.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.repository.TaskRepository
import com.xephyrka.liora.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles the "Quick Add" functionality from the system notification shade.
 * Processes remote input text and saves it as a new Task or Note in the database.
 */
class QuickAddReceiver : BroadcastReceiver() {

    /**
     * Called when the user submits text via the Quick Add notification action.
     * Extracts the text, determines if it's a task or note, and persists it asynchronously.
     */
    override fun onReceive(context: Context, intent: Intent) {
        /** Bundle containing the results from the RemoteInput field in the notification. */
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            /** The raw text entered by the user in the notification input. */
            val text = remoteInput.getCharSequence("quick_add_text")?.toString()
            /** Boolean indicating whether the user clicked "Add Note" (true) or "Add Task" (false). */
            val isNote = intent.getBooleanExtra("is_note", false)
            
            if (!text.isNullOrBlank()) {
                /** Repository instance for database interaction. */
                val repository = TaskRepository(context)
                /** Helper for updating the notification state after processing. */
                val notificationHelper = NotificationHelper(context)
                
                CoroutineScope(Dispatchers.IO).launch {
                    /** The new entity to be saved. ListId -1 represents the 'Inbox' or unassigned category. */
                    val newTask = Task(
                        title = if (isNote) "Quick Note" else text,
                        content = if (isNote) text else "",
                        listId = -1, 
                        itemType = if (isNote) ItemType.NOTE else ItemType.TASK
                    )
                    repository.insertTask(newTask)
                    
                    notificationHelper.showQuickAddNotification()
                }
            }
        }
    }
}
