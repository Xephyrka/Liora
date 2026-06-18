package com.xephyrka.liora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xephyrka.liora.data.model.RecurrenceUnit
import com.xephyrka.liora.ui.theme.AppOutlineWidth

/**
 * A dialog for configuring a task's recurrence rules.
 * Users can specify the frequency interval (e.g., every 2) and the unit (days, weeks, months).
 * For weekly recurrence, users can also select specific days of the week.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurrencePickerDialog(
    /** Callback triggered when the dialog is dismissed. */
    onDismiss: () -> Unit,
    /** 
     * Callback triggered when the user confirms their recurrence settings.
     * Passes the interval, unit, selected days of week, end date, and max occurrences.
     */
    onConfirm: (
        interval: Int,
        unit: RecurrenceUnit,
        daysOfWeek: String?,
        endDate: Long?,
        endOccurrences: Int?
    ) -> Unit,
    /** The initial recurrence frequency interval. */
    initialInterval: Int = 1,
    /** The initial recurrence time unit. */
    initialUnit: RecurrenceUnit = RecurrenceUnit.DAY,
    /** Comma-separated string of day indices for initial weekly recurrence. */
    initialDaysOfWeek: String? = null
) {
    /** Local state for the frequency interval (e.g., '1' for every day). */
    var interval by remember { mutableIntStateOf(initialInterval) }
    /** Local state for the recurrence time unit (DAY, WEEK, or MONTH). */
    var unit by remember { mutableStateOf(initialUnit) }
    /** Local state tracking selected days for weekly recurrence. */
    var selectedDays by remember {
        mutableStateOf(
            initialDaysOfWeek?.split(",")?.filter { it.isNotBlank() }?.map { it.toInt() }?.toSet() ?: emptySet()
        )
    }

    /** Labels for the days of the week. */
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repeats") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Every")
                    OutlinedTextField(
                        value = if (interval == 0) "" else interval.toString(),
                        onValueChange = { interval = it.toIntOrNull() ?: 0 },
                        modifier = Modifier.width(72.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    /** Controls visibility of the recurrence unit dropdown. */
                    var unitExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { unitExpanded = true }) {
                            val unitStr = when(unit) {
                                RecurrenceUnit.DAY -> if (interval > 1) "days" else "day"
                                RecurrenceUnit.WEEK -> if (interval > 1) "weeks" else "week"
                                RecurrenceUnit.MONTH -> if (interval > 1) "months" else "month"
                            }
                            Text(unitStr)
                        }
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            listOf(RecurrenceUnit.DAY, RecurrenceUnit.WEEK, RecurrenceUnit.MONTH).forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (unit == RecurrenceUnit.WEEK) {
                    Text("Repeat on", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEachIndexed { index, day ->
                            /** Unique value (1-7) for each day of the week. */
                            val dayValue = index + 1
                            /** Whether this day is currently selected. */
                            val isSelected = selectedDays.contains(dayValue)
                            
                            val shape = MaterialTheme.shapes.small
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(shape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = AppOutlineWidth,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.outline,
                                        shape = shape
                                    )
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            selectedDays - dayValue
                                        } else {
                                            selectedDays + dayValue
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onConfirm(0, RecurrenceUnit.DAY, null, null, null)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
                
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            onConfirm(
                                if (interval < 1) 1 else interval,
                                unit,
                                if (unit == RecurrenceUnit.WEEK) selectedDays.sorted().joinToString(",") else null,
                                null,
                                null
                            )
                        }
                    ) { Text("Done") }
                }
            }
        },
        dismissButton = null,    )
}
