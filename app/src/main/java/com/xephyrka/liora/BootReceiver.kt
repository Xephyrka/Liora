package com.xephyrka.liora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xephyrka.liora.viewmodel.TaskViewModel

/**
 * BroadcastReceiver that listens for the device's boot completion event.
 * Ensures that all scheduled task alarms are restored and persistent notifications are re-shown after a reboot.
 */
class BootReceiver : BroadcastReceiver() {

    /**
     * Called when the system broadcasts [Intent.ACTION_BOOT_COMPLETED].
     * Triggers alarm rescheduling and initializes persistent UI elements.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            /** Instance of the TaskViewModel initialized for background work. */
            val viewModel = TaskViewModel(context.applicationContext as android.app.Application)
            viewModel.rescheduleAllAlarms()
            
            /** Utility for re-displaying the persistent quick-add notification. */
            NotificationHelper(context).showQuickAddNotification()
        }
    }
}
