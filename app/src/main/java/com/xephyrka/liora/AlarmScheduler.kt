package com.xephyrka.liora

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xephyrka.liora.data.model.Task

/**
 * Helper class for scheduling and canceling alarms related to task reminders.
 * Interfaces with the Android system's AlarmManager.
 */
class AlarmScheduler(private val context: Context) {

    /** Instance of the system AlarmManager service. */
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** 
     * Schedules an exact alarm for a given task's reminder time.
     * Uses the task's unique ID as the request code to manage multiple alarms.
     */
    fun schedule(task: Task) {
        cancel(task) // Cancel any existing alarm for this task before scheduling a new one

        /** Intent that will be broadcast to the AlarmReceiver when the alarm triggers. */
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
        }

        /** PendingIntent wrapper for the alarm broadcast. */
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        task.reminderTime?.let {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                it,
                pendingIntent
            )
        }
    }

    /** 
     * Cancels any currently scheduled alarm for the specified task.
     * Uses the task's ID to identify the correct PendingIntent to cancel.
     */
    fun cancel(task: Task) {
        /** Intent matching the one used to schedule the alarm. */
        val intent = Intent(context, AlarmReceiver::class.java)
        /** PendingIntent matching the one used to schedule the alarm. */
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }
}
