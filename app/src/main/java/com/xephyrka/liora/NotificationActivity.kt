package com.xephyrka.liora

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.xephyrka.liora.ui.screens.NotificationScreen
import com.xephyrka.liora.ui.theme.LioraTheme
import com.xephyrka.liora.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Activity responsible for displaying the full-screen reminder when a task alarm triggers.
 * It handles unlocking the screen, playing the alert via [NotificationScreen], and 
 * processing user actions like snoozing or completing the task.
 */
class NotificationActivity : ComponentActivity() {
    /** ViewModel instance for task state management. */
    private val taskViewModel: TaskViewModel by viewModels()
    
    /** Internal state for the ID of the task currently being notified. */
    private var currentTaskId by mutableIntStateOf(-1)
    /** Internal state for the title of the task being notified. */
    private var taskTitle by mutableStateOf("")

    /**
     * Initializes the activity, requests screen-on/keyguard dismissal, and sets up the Compose UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndKeyguardOff()
        extractData(intent)

        setContent {
            LioraTheme {
                NotificationScreen(
                    taskTitle = taskTitle,
                    onStart = { 
                        dismissEverything()
                    },
                    onSnooze = {
                        if (currentTaskId != -1) {
                            lifecycleScope.launch {
                                taskViewModel.getTaskById(currentTaskId)?.let { task ->
                                    val alarmScheduler = AlarmScheduler(applicationContext)
                                    val snoozeTime = Calendar.getInstance().apply {
                                        add(Calendar.MINUTE, 10)
                                    }.timeInMillis
                                    alarmScheduler.schedule(task.copy(reminderTime = snoozeTime))
                                }
                            }
                        }
                        dismissEverything()
                    },
                    onDone = {
                        if (currentTaskId != -1) {
                            lifecycleScope.launch {
                                taskViewModel.getTaskById(currentTaskId)?.let { task ->
                                    taskViewModel.onTaskCompletionToggled(task)
                                    taskViewModel.confirmTaskDeletion()
                                }
                            }
                        }
                        dismissEverything()
                    },
                    onDismiss = { 
                        dismissEverything()
                    }
                )
            }
        }
    }

    /** 
     * Dismisses the reminder UI by canceling the system notification and finishing the activity.
     */
    private fun dismissEverything() {
        cancelNotification()
        finish()
    }

    /** 
     * Removes the persistent notification associated with the current task from the status bar.
     */
    private fun cancelNotification() {
        if (currentTaskId != -1) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(currentTaskId)
        }
    }

    /** 
     * Handles new intents if the activity is already running (e.g., another alarm triggers).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractData(intent)
    }

    /** 
     * Extracts task metadata from the provided intent.
     */
    private fun extractData(intent: Intent?) {
        currentTaskId = intent?.getIntExtra("taskId", -1) ?: -1
        taskTitle = intent?.getStringExtra("taskTitle") ?: ""
    }

    /** 
     * Configures the activity to show over the lock screen and turn on the device's display.
     */
    private fun turnScreenOnAndKeyguardOff() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)
    }
}
