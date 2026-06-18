package com.xephyrka.liora.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xephyrka.liora.data.model.TaskList

/**
 * Data class holding basic information about an installed application.
 * Used for populating the app selection dialog in focus mode settings.
 */
data class AppInfo(
    /** The user-visible label of the application. */
    val name: String,
    /** The unique package name identifier for the application. */
    val packageName: String,
    /** The system icon for the application. */
    val icon: Drawable,
)

/**
 * A dialog allowing users to select multiple applications from a list of installed apps.
 * Used to define which applications should be restricted or grayscaled during focus periods.
 */
@Composable
fun AppPickerDialog(
    /** Callback triggered when the dialog is dismissed without saving. */
    onDismiss: () -> Unit,
    /** The full list of apps available on the device. */
    apps: List<AppInfo>,
    /** The set of package names currently selected. */
    selectedApps: Set<String>,
    /** Callback triggered when the user confirms their selection. */
    onAppsSelected: (Set<String>) -> Unit,
) {
    /** Local state tracking the current selection within the dialog. */
    var currentSelection by remember { mutableStateOf(selectedApps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Apps to Restrict") },
        text = {
            Box(modifier = Modifier.height(400.dp)) {
                LazyColumn {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    currentSelection = if (currentSelection.contains(app.packageName)) {
                                        currentSelection - app.packageName
                                    } else {
                                        currentSelection + app.packageName
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = currentSelection.contains(app.packageName),
                                onCheckedChange = null,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(app.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAppsSelected(currentSelection) }, shape = MaterialTheme.shapes.medium) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.medium) {
                Text("Cancel")
            }
        },
    )
}

/**
 * A dialog allowing the user to select a specific [TaskList] to categorize a task.
 * Includes a "None (Inbox)" option for unassigned tasks.
 */
@Composable
fun ListPickerDialog(
    /** Callback triggered when the dialog is dismissed. */
    onDismiss: () -> Unit,
    /** The list of available task categories. */
    lists: List<TaskList>,
    /** The ID of the currently selected list. */
    selectedListId: Int,
    /** Callback triggered when a list is selected. */
    onListSelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select List") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onListSelected(-1)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedListId == -1,
                                onClick = null,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("None (Inbox)", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    items(lists, key = { it.id }) { list ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onListSelected(list.id)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = list.id == selectedListId,
                                onClick = null,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(list.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.medium) {
                Text("Close")
            }
        },
    )
}
