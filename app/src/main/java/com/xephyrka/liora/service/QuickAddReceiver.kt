package com.xephyrka.liora.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.xephyrka.liora.AlarmScheduler
import com.xephyrka.liora.data.model.Task
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.repository.TaskRepository
import com.xephyrka.liora.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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
                val alarmScheduler = AlarmScheduler(context)
                
                CoroutineScope(Dispatchers.IO).launch {
                    var finalTitle = text
                    var reminderTime: Long? = null

                    if (!isNote && text.contains(";")) {
                        val parts = text.split(";")
                        if (parts.size >= 2) {
                            val parsedTime = parseTime(parts.last().trim())
                            if (parsedTime != null) {
                                finalTitle = parts.dropLast(1).joinToString(";").trim()
                                reminderTime = parsedTime
                            }
                        }
                    }

                    /** The new entity to be saved. ListId -1 represents the 'Inbox' or unassigned category. */
                    val newTask = Task(
                        title = if (isNote) "Quick Note" else finalTitle,
                        content = if (isNote) text else "",
                        listId = -1, 
                        itemType = if (isNote) ItemType.NOTE else ItemType.TASK,
                        reminderTime = reminderTime
                    )
                    val taskId = repository.insertTask(newTask)
                    
                    if (reminderTime != null) {
                        alarmScheduler.schedule(newTask.copy(id = taskId.toInt()))
                    }
                    
                    notificationHelper.showQuickAddNotification()
                }
            }
        }
    }

    /**
     * Parses a time string (e.g., "5:00 PM", "17:00", "5pm") into a timestamp for today or tomorrow.
     */
    private fun parseTime(timeStr: String): Long? {
        val normalized = timeStr.lowercase(Locale.getDefault()).replace(" ", "")
        val timeRegex = Regex("(\\d{1,2}):?(\\d{2})?(am|pm)?")
        val match = timeRegex.matchEntire(normalized) ?: return null

        var hours = match.groupValues[1].toInt()
        val minutes = if (match.groupValues[2].isNotEmpty()) match.groupValues[2].toInt() else 0
        val amPm = match.groupValues[3]

        if (amPm == "pm" && hours < 12) hours += 12
        if (amPm == "am" && hours == 12) hours = 0

        if (hours > 23 || minutes > 59) return null

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
