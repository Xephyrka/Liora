@file:OptIn(ExperimentalMaterial3Api::class)

package com.xephyrka.liora.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.model.SubTask
import com.xephyrka.liora.data.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A Composable that displays a single task or note within a card.
 * Handles different layouts for notes (content display) and tasks (checkboxes and subtasks).
 * Includes entry and exit animations for task completion.
 */
@Composable
fun TaskItem(
    /** The task entity to be displayed. */
    task: Task,
    /** The list of subtasks associated with this task. */
    subtasks: List<SubTask>,
    /** Callback triggered when the main task's completion status is toggled. */
    onToggle: () -> Unit,
    /** Callback triggered when the user clicks the card to edit the task. */
    onEdit: () -> Unit,
    /** Callback for deleting the task. Currently suppressed as it's handled via the edit screen. */
    @Suppress("UNUSED_PARAMETER") onDelete: () -> Unit,
    /** Callback triggered when a subtask's completion status is toggled. */
    onSubTaskToggle: (SubTask) -> Unit,
) {
    /** Access to system resources for time formatting. */
    val context = LocalContext.current
    /** Animated scale factor that shrinks the card slightly when completed. */
    val scale by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.8f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "TaskScale"
    )
    
    /** Animated alpha factor that fades out the card when completed. */
    val alpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "TaskAlpha"
    )

    /** 
     * Orchestrates the visibility of the task card.
     * When task.isCompleted becomes true, this triggers the exit animation.
     */
    AnimatedVisibility(
        visible = !task.isCompleted,
        exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(animationSpec = tween(500)),
        enter = fadeIn() + expandVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
                .clickable { onEdit() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { onEdit() }
                .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (task.itemType != ItemType.NOTE && task.reminderTime != null) {
                            /** Boolean indicating if the system uses 24-hour time. */
                            val is24Hour = DateFormat.is24HourFormat(context)
                            /** The time format pattern based on system settings. */
                            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                            /** Formatter for displaying the reminder time. */
                            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                            
                            val recurrenceText = if (task.isRecurring) {
                                when (task.recurrenceUnit) {
                                    com.xephyrka.liora.data.model.RecurrenceUnit.DAY -> if (task.recurrenceInterval == 1) " (Daily)" else " (Every ${task.recurrenceInterval} days)"
                                    com.xephyrka.liora.data.model.RecurrenceUnit.WEEK -> if (task.recurrenceInterval == 1) " (Weekly)" else " (Every ${task.recurrenceInterval} weeks)"
                                    com.xephyrka.liora.data.model.RecurrenceUnit.MONTH -> if (task.recurrenceInterval == 1) " (Monthly)" else " (Every ${task.recurrenceInterval} months)"
                                }
                            } else ""

                            Text(
                                text = "Start at: ${sdf.format(Date(task.reminderTime))}$recurrenceText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (task.itemType != ItemType.NOTE) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { _ -> onToggle() },
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .testTag("TaskCheckbox_${task.id}"),
                        )
                    }
                }

                if (task.itemType == ItemType.NOTE) {
                    if (task.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = task.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (subtasks.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (subtask in subtasks) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = subtask.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = subtask.isCompleted,
                                        onCheckedChange = { _ -> onSubTaskToggle(subtask) },
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
