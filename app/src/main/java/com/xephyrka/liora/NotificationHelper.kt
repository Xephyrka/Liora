package com.xephyrka.liora

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.xephyrka.liora.service.QuickAddReceiver
import com.xephyrka.liora.data.PreferenceManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Utility class for creating and managing app notifications.
 * Handles notification channels, full-screen reminder notifications, and persistent quick-add notifications.
 */
class NotificationHelper(private val context: Context) {

    /** Unique identifier for the primary alarm/reminder notification channel. */
    private val channelId = "liora_alarms_v3"
    /** Unique identifier for the quick-add persistent notification channel. */
    private val quickAddChannelId = "liora_quick_add"
    /** User-visible name for the primary alarm notification channel. */
    private val channelName = "Liora Alarms"
    /** User-visible name for the quick-add notification channel. */
    private val quickAddChannelName = "Quick Add Task"
    /** Manager for accessing and updating user preferences. */
    private val preferenceManager = PreferenceManager(context)

    init {
        createNotificationChannel()
    }

    /** 
     * Creates the necessary notification channels for the app.
     * Also handles cleaning up deprecated channels from older app versions.
     */
    private fun createNotificationChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.deleteNotificationChannel("liora_channel_high")
        notificationManager.deleteNotificationChannel("liora_alarms_channel_v1")
        notificationManager.deleteNotificationChannel("liora_alarms_v2")

        val alarmChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Critical Liora Task Reminders"
            setBypassDnd(true)
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(alarmChannel)

        val quickAddChannel = NotificationChannel(quickAddChannelId, quickAddChannelName, NotificationManager.IMPORTANCE_LOW).apply {
            description = "Persistent notification for quickly adding tasks"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(quickAddChannel)
    }

    /** 
     * Displays a notification for a task reminder.
     * Can be configured as a high-priority full-screen intent if enabled in settings.
     */
    @SuppressLint("FullScreenIntentPolicy")
    fun showFullScreenNotification(taskId: Int, taskTitle: String) {
        val isEnabled = runBlocking { 
            preferenceManager.fullScreenNotificationsEnabledFlow.first() 
        }

        /** Intent that opens the NotificationActivity when the notification is interacted with. */
        val intent = Intent(context, NotificationActivity::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        /** PendingIntent wrapper for the notification content click. */
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** Builder for constructing the reminder notification. */
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Urgent: Start your task!")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isEnabled) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(taskId, builder.build())
    }

    /** 
     * Displays a persistent notification that allows the user to quickly add tasks or notes.
     * Uses RemoteInput for direct text entry from the notification shade.
     */
    fun showQuickAddNotification() {
        /** Component that handles text input directly within the notification. */
        val remoteInput = RemoteInput.Builder("quick_add_text")
            .setLabel("Type something...")
            .build()

        /** Intent sent when the "Add Task" action is triggered. */
        val taskIntent = Intent(context, QuickAddReceiver::class.java).apply {
            putExtra("is_note", false)
        }
        /** PendingIntent for the "Add Task" action. */
        val taskPendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            taskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        /** Action button for adding a new task. */
        val taskAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            "Add Task",
            taskPendingIntent
        ).addRemoteInput(remoteInput).build()

        /** Intent sent when the "Add Note" action is triggered. */
        val noteIntent = Intent(context, QuickAddReceiver::class.java).apply {
            putExtra("is_note", true)
        }
        /** PendingIntent for the "Add Note" action. */
        val notePendingIntent = PendingIntent.getBroadcast(
            context,
            998,
            noteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        /** Action button for adding a new note. */
        val noteAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "Add Note",
            notePendingIntent
        ).addRemoteInput(remoteInput).build()

        /** Intent that opens the MainActivity when the notification body is clicked. */
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** Builder for the persistent quick-add notification. */
        val builder = NotificationCompat.Builder(context, quickAddChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Liora Quick Add")
            .setContentText("Capture a thought before it's gone.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(taskAction)
            .addAction(noteAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(999, builder.build())
    }
}
