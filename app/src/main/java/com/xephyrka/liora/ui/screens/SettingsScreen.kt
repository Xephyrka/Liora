@file:Suppress("AssignedValueIsNeverRead")

package com.xephyrka.liora.ui.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.xephyrka.liora.data.PreferenceManager
import com.xephyrka.liora.ui.components.AppInfo
import com.xephyrka.liora.ui.components.AppPickerDialog
import com.xephyrka.liora.ui.components.CupertinoGroupedSection
import com.xephyrka.liora.ui.components.SettingsClickableItem
import com.xephyrka.liora.ui.components.SettingsNavigationItem
import com.xephyrka.liora.ui.components.SettingsSection
import com.xephyrka.liora.ui.components.SettingsToggleItem
import com.xephyrka.liora.viewmodel.TaskViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.xephyrka.liora.ui.theme.LioraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The main entry point for the Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    externalPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: TaskViewModel,
    preferenceManager: PreferenceManager,
    darkMode: Boolean,
    useSystemTheme: Boolean,
    userName: String,
    notificationSound: String?,
    developerMode: Boolean,
    blockedApps: Set<String>,
    startHour: Int,
    endHour: Int,
    grayscaleEnabled: Boolean,
    fullScreenEnabled: Boolean,
    onNotificationSoundClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = viewModel.getExportDataJson()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val json = stream.bufferedReader().use { reader -> reader.readText() }
                        viewModel.importDataFromJson(json)
                    }
                    Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    SettingsContent(
        externalPadding = externalPadding,
        userName = userName,
        darkMode = darkMode,
        useSystemTheme = useSystemTheme,
        notificationSound = notificationSound,
        developerMode = developerMode,
        blockedApps = blockedApps,
        startHour = startHour,
        endHour = endHour,
        grayscaleEnabled = grayscaleEnabled,
        fullScreenEnabled = fullScreenEnabled,
        onUserNameSave = { scope.launch { preferenceManager.setUserName(it) } },
        onResetData = {
            scope.launch {
                viewModel.clearAllData()
                preferenceManager.clearPreferences()
            }
        },
        onAppsSelected = { scope.launch { preferenceManager.setBlockedApps(it) } },
        onGrayscaleToggle = { scope.launch { preferenceManager.setGrayscaleEnabled(it) } },
        onUseSystemThemeToggle = { scope.launch { preferenceManager.setUseSystemTheme(it) } },
        onDarkModeToggle = { scope.launch { preferenceManager.setDarkMode(it) } },
        onNotificationSoundClick = onNotificationSoundClick,
        onFullScreenToggle = { scope.launch { preferenceManager.setFullScreenNotificationsEnabled(it) } },
        onExportData = { exportLauncher.launch("liora_backup.json") },
        onImportData = { importLauncher.launch(arrayOf("application/json")) },
        onDeveloperModeEnable = {
            scope.launch {
                preferenceManager.setDeveloperMode(enabled = true)
                Toast.makeText(context, "Developer mode enabled!", Toast.LENGTH_SHORT).show()
            }
        },
        onTestNotification = {
            val intent = Intent(context, com.xephyrka.liora.NotificationActivity::class.java).apply {
                putExtra("taskId", -1)
                putExtra("taskTitle", "Sample Test Task")
                putExtra("firstStep", "This is how your reminder will look!")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        onGithubClick = {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Xephyrka/Liora".toUri())
            context.startActivity(intent)
        },
        onSetHours = { start, end ->
             scope.launch {
                 preferenceManager.setBlockingHours(start, end)
             }
        },
        onResetOnboarding = {
            scope.launch {
                preferenceManager.setOnboardingShown(false)
                Toast.makeText(context, "Onboarding reset. Restart app to see it.", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    externalPadding: PaddingValues = PaddingValues(0.dp),
    userName: String,
    darkMode: Boolean,
    useSystemTheme: Boolean,
    notificationSound: String?,
    developerMode: Boolean,
    blockedApps: Set<String>,
    startHour: Int,
    endHour: Int,
    grayscaleEnabled: Boolean,
    fullScreenEnabled: Boolean,
    onUserNameSave: (String) -> Unit,
    onResetData: () -> Unit,
    onAppsSelected: (Set<String>) -> Unit,
    onGrayscaleToggle: (Boolean) -> Unit,
    onUseSystemThemeToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onNotificationSoundClick: () -> Unit,
    onFullScreenToggle: (Boolean) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onDeveloperModeEnable: () -> Unit,
    onTestNotification: () -> Unit,
    onGithubClick: () -> Unit,
    onSetHours: (Int, Int) -> Unit,
    onResetOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showHoursPicker by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }
    val currentYear = Calendar.getInstance()[Calendar.YEAR]

    if (showHoursPicker) {
        TimeRangePickerDialog(
            startHour = startHour,
            endHour = endHour,
            onDismiss = { showHoursPicker = false },
            onConfirm = { start, end ->
                onSetHours(start, end)
                showHoursPicker = false
            }
        )
    }

    val apps by produceState<List<AppInfo>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map {
                    AppInfo(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.packageName,
                        icon = it.loadIcon(pm)
                    )
                }
                .filter { it.packageName != context.packageName }
                .sortedBy { it.name.lowercase() }
                .toList()
        }
    }

    // Improved app feedback
    val selectedAppNames = remember(blockedApps, apps) {
        blockedApps.mapNotNull { pkg -> apps.find { it.packageName == pkg }?.name }
    }
    val appsSubtitle = when {
        selectedAppNames.isEmpty() -> "Select apps to restrict"
        selectedAppNames.size <= 2 -> selectedAppNames.joinToString(", ")
        else -> "${selectedAppNames.take(2).joinToString(", ")} +${selectedAppNames.size - 2}"
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Set Your Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUserNameSave(tempName)
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently delete all your tasks and settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            apps = apps,
            selectedApps = blockedApps,
            onAppsSelected = { 
                onAppsSelected(it)
                showAppPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        }
    ) { innerPadding ->
        val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
        val timeRange by remember(startHour, endHour, is24Hour) {
            derivedStateOf {
                val pattern = if (is24Hour) "HH:mm" else "h a"
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                val startCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, 0) }
                val endCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, 0) }
                "${sdf.format(startCal.time)} - ${sdf.format(endCal.time)}"
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(bottom = externalPadding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SettingsSection(title = "Profile") {
                    CupertinoGroupedSection {
                        SettingsNavigationItem(
                            title = "Your Name",
                            subtitle = userName.ifBlank { "Set a name" },
                            icon = Icons.Default.AccountCircle,
                            onClick = { 
                                tempName = userName
                                showNameDialog = true 
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Focus Mode") {
                    CupertinoGroupedSection {
                        SettingsNavigationItem(
                            title = "Selected Apps",
                            subtitle = appsSubtitle,
                            icon = Icons.Default.Lock,
                            onClick = { showAppPicker = true }
                        )
                        
                        SettingsToggleItem(
                            title = "Automatic Grayscale",
                            subtitle = "Grayscale when using selected apps",
                            icon = Icons.Default.Edit,
                            checked = grayscaleEnabled,
                            onCheckedChange = onGrayscaleToggle
                        )

                        SettingsNavigationItem(
                            title = "Active Hours",
                            subtitle = timeRange,
                            icon = Icons.Default.DateRange,
                            onClick = { showHoursPicker = true }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    CupertinoGroupedSection {
                        SettingsToggleItem(
                            title = "Use System Theme",
                            subtitle = "Match system theme settings",
                            icon = Icons.Default.Settings,
                            checked = useSystemTheme,
                            onCheckedChange = onUseSystemThemeToggle
                        )
                        
                        if (!useSystemTheme) {
                            SettingsToggleItem(
                                title = "Dark Mode",
                                subtitle = "Manual dark theme",
                                icon = Icons.Default.Star,
                                checked = darkMode,
                                onCheckedChange = onDarkModeToggle
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Notifications") {
                    CupertinoGroupedSection {
                        SettingsNavigationItem(
                            title = "Notification Sound",
                            subtitle = if (notificationSound == null) "Default" else {
                                try {
                                    val ringtone = RingtoneManager.getRingtone(context, notificationSound.toUri())
                                    ringtone.getTitle(context)
                                } catch (_: Exception) { "Unknown" }
                            },
                            icon = Icons.Default.Notifications,
                            onClick = onNotificationSoundClick
                        )

                        SettingsToggleItem(
                            title = "Full-Screen Reminders",
                            subtitle = "Show reminders in full screen",
                            icon = Icons.Default.Warning,
                            checked = fullScreenEnabled,
                            onCheckedChange = onFullScreenToggle
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Data Management") {
                    CupertinoGroupedSection {
                        SettingsClickableItem(
                            title = "Export Data",
                            subtitle = "Save tasks to a file",
                            icon = Icons.Default.Share,
                            onClick = onExportData
                        )
                        SettingsClickableItem(
                            title = "Import Data",
                            subtitle = "Restore tasks from backup",
                            icon = Icons.Default.Refresh,
                            onClick = onImportData
                        )
                        SettingsClickableItem(
                            title = "Reset App",
                            subtitle = "Clear everything",
                            icon = Icons.Default.Delete,
                            onClick = { showDeleteConfirm = true }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "About") {
                    CupertinoGroupedSection {
                        SettingsNavigationItem(
                            title = "Version",
                            subtitle = "1.0.2",
                            icon = Icons.Default.Info,
                            onClick = {
                                if (!developerMode) {
                                    versionTapCount++
                                    if (versionTapCount >= 10) onDeveloperModeEnable()
                                }
                            }
                        )
                        SettingsNavigationItem(
                            title = "GitHub Project",
                            subtitle = "View source code",
                            icon = Icons.Default.Favorite,
                            onClick = onGithubClick
                        )
                    }
                }
            }

            if (developerMode) {
                item {
                    SettingsSection(title = "Developer Tools") {
                        CupertinoGroupedSection {
                            SettingsClickableItem(
                                title = "Test Notification",
                                subtitle = "Trigger reminder immediately",
                                icon = Icons.Default.Build,
                                onClick = onTestNotification
                            )
                            SettingsClickableItem(
                                title = "Reset Onboarding",
                                subtitle = "Show guide again on next launch",
                                icon = Icons.Default.Refresh,
                                onClick = onResetOnboarding
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

/**
 * A simple dialog to select the start and end hours for focus mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangePickerDialog(
    startHour: Int,
    endHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val startState = rememberTimePickerState(initialHour = startHour, initialMinute = 0, is24Hour = is24Hour)
    val endState = rememberTimePickerState(initialHour = endHour, initialMinute = 0, is24Hour = is24Hour)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Active Hours") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeInput(state = startState)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("End Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeInput(state = endState)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(startState.hour, endState.hour) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**Preview for the Settings screen to preview the screen, without building the whole project again and again. */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    LioraTheme {
        SettingsContent(
            userName = "John Doe",
            darkMode = false,
            useSystemTheme = true,
            notificationSound = null,
            developerMode = true,
            blockedApps = setOf("com.android.chrome", "com.google.android.youtube"),
            startHour = 6,
            endHour = 12,
            grayscaleEnabled = true,
            fullScreenEnabled = true,
            onUserNameSave = {},
            onResetData = {},
            onAppsSelected = {},
            onGrayscaleToggle = {},
            onUseSystemThemeToggle = {},
            onDarkModeToggle = {},
            onNotificationSoundClick = {},
            onFullScreenToggle = {},
            onExportData = {},
            onImportData = {},
            onDeveloperModeEnable = {},
            onTestNotification = {},
            onGithubClick = {},
            onSetHours = { _, _ -> },
            onResetOnboarding = {}
        )
    }
}
