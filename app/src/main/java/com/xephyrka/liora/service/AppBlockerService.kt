@file:Suppress("SpellCheckingInspection")

package com.xephyrka.liora.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.xephyrka.liora.BlockActivity
import com.xephyrka.liora.data.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * AccessibilityService that monitors app usage to implement focus mode features.
 * It handles automatic grayscale switching and blocks access to restricted apps during specified hours.
 */
@SuppressLint("AccessibilityPolicy")
class AppBlockerService : AccessibilityService() {

    /** SupervisorJob for managing the lifecycle of coroutines in this service. */
    private val job = SupervisorJob()
    /** Coroutine scope bound to the service's lifecycle and the main dispatcher. */
    private val scope = CoroutineScope(Dispatchers.Main + job)
    /** Manager for accessing user-defined blocking settings and restricted app lists. */
    private lateinit var preferenceManager: PreferenceManager
    /** The package name of the application currently in the foreground. */
    private var currentActiveApp: String? = null
    
    /** The most recently applied state of the system daltonizer (grayscale). */
    private var lastAppliedGrayscaleState: Boolean? = null
    /** Handle to the coroutine processing the most recent application switch. */
    private var appSwitchJob: Job? = null

    /** Set of system package names that should not trigger blocking logic to maintain system stability. */
    private val systemPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.google.android.googlequicksearchbox",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.markup",
        "com.google.ar.lens",
        "android"
    )

    /** 
     * Initializes the preference manager when the service is created.
     */
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }

    /** 
     * Responds to accessibility events, specifically window state changes, to detect app switches.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        
        val packageName = event.packageName?.toString() ?: return

        // Always disable grayscale for the app itself or system packages (Home, Lens, etc.)
        if (packageName == "com.xephyrka.liora" || systemPackages.contains(packageName)) {
            setTrueGrayscale(false)
            currentActiveApp = packageName
            return
        }

        if (packageName != currentActiveApp) {
            currentActiveApp = packageName
            
            appSwitchJob?.cancel()
            appSwitchJob = scope.launch {
                delay(150)
                processAppSwitch(packageName)
            }
        }
    }

    /** 
     * Processes an application switch by checking against blocking rules and grayscale preferences.
     */
    private fun processAppSwitch(packageName: String) {
        scope.launch {
            val blockedApps = preferenceManager.blockedAppsFlow.first()
            val isGrayscaleEnabled = preferenceManager.grayscaleEnabledFlow.first()
            val startHour = preferenceManager.blockingStartHourFlow.first()
            val endHour = preferenceManager.blockingEndHourFlow.first()

            val isSelectedApp = blockedApps.contains(packageName)
            val isBlockingTime = isWithinBlockingTime(startHour, endHour)

            if (isGrayscaleEnabled) {
                // If it's blocking time, we don't grayscale because the blocker screen will be shown.
                // Otherwise, grayscale if the app is one of the selected/blocked apps.
                if (isBlockingTime && isSelectedApp) {
                    setTrueGrayscale(false)
                } else {
                    setTrueGrayscale(isSelectedApp)
                }
            }

            if (isSelectedApp && isBlockingTime) {
                if (currentActiveApp == packageName) {
                    val intent = Intent(this@AppBlockerService, BlockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("blocked_package", packageName)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("AppBlocker", "Failed to start BlockActivity: ${e.message}")
                    }
                }
            }
        }
    }

    /** 
     * Enables or disables the system grayscale mode using the Secure Settings API.
     * Requires the WRITE_SECURE_SETTINGS permission granted via ADB.
     */
    private fun setTrueGrayscale(enabled: Boolean) {
        if (lastAppliedGrayscaleState == enabled) return
        
        try {
            val value = if (enabled) 1 else 0
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", value)
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", if (enabled) 0 else -1)
            lastAppliedGrayscaleState = enabled
        } catch (e: Exception) {
            Log.e("AppBlocker", "Grayscale failed: ${e.message}")
        }
    }

    /** 
     * Determines if the current time falls within the user's defined blocking window.
     * Handles windows that cross over the midnight boundary.
     */
    private fun isWithinBlockingTime(startHour: Int, endHour: Int): Boolean {
        val currentHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        return if (startHour < endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour !in endHour until startHour
        }
    }

    /** 
     * Required implementation for AccessibilityService; not used in this context.
     */
    override fun onInterrupt() {}

    /** 
     * Cleans up resources and ensures grayscale is disabled when the service is stopped.
     */
    override fun onDestroy() {
        super.onDestroy()
        setTrueGrayscale(enabled = false)
        job.cancel()
    }
}
