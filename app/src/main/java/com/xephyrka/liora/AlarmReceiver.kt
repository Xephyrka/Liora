package com.xephyrka.liora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * BroadcastReceiver responsible for handling scheduled task reminders.
 * Triggered by the AlarmManager, it initiates notifications and direct activity launches.
 */
class AlarmReceiver : BroadcastReceiver() {

    /**
     * Called when the scheduled alarm is triggered.
     * Extracts task data from the intent and attempts to notify the user via a full-screen notification or direct activity launch.
     */
    override fun onReceive(context: Context, intent: Intent) {
        /** The unique identifier of the task for which the alarm was triggered. */
        val taskId = intent.getIntExtra("taskId", -1)
        /** The title of the task to be displayed in the reminder. */
        val taskTitle = intent.getStringExtra("taskTitle") ?: ""
        
        /** Helper for managing and displaying application notifications. */
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showFullScreenNotification(taskId, taskTitle)

        if (Settings.canDrawOverlays(context)) {
            /** Intent used to launch the NotificationActivity directly over other apps. */
            val activityIntent = Intent(context, NotificationActivity::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("taskTitle", taskTitle)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(activityIntent)
        }
    }
}
