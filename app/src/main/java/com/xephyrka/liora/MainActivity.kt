package com.xephyrka.liora

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.app.AlarmManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.xephyrka.liora.navigation.MainScreen
import com.xephyrka.liora.ui.theme.LioraTheme
import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.net.toUri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.xephyrka.liora.data.PreferenceManager

/**
 * The main activity and entry point of the Liora application.
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val preferenceManager = remember { PreferenceManager(this) }
            val darkMode by preferenceManager.darkModeFlow.collectAsState(initial = false)
            val dynamicColor by preferenceManager.dynamicColorFlow.collectAsState(initial = false)
            val useSystemTheme by preferenceManager.useSystemThemeFlow.collectAsState(initial = true)

            var showAccessibilityDialog by remember { mutableStateOf(false) }

            LioraTheme(
                darkTheme = if (useSystemTheme) androidx.compose.foundation.isSystemInDarkTheme() else darkMode,
                dynamicColor = dynamicColor,
            ) {
                if (showAccessibilityDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showAccessibilityDialog = false },
                        title = { androidx.compose.material3.Text("Accessibility Required") },
                        text = { androidx.compose.material3.Text("Liora needs Accessibility Service to block apps and apply grayscale. Please enable 'Liora Blocker' in the settings") },
                        confirmButton = {
                            androidx.compose.material3.Button(onClick = {
                                showAccessibilityDialog = false
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }) { androidx.compose.material3.Text("Go to Settings") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showAccessibilityDialog = false }) { androidx.compose.material3.Text("Later") }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onRequestPermissions = { 
                            checkPermissions()
                            if (!isAccessibilityServiceEnabled()) {
                                showAccessibilityDialog = true
                            }
                        }
                    )
                }
            }
        }

        NotificationHelper(this).showQuickAddNotification()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName == packageName && enabledServiceInfo.name == "com.xephyrka.liora.service.AppBlockerService") {
                return true
            }
        }
        return false
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Only request if not already granted
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }
    }
}
