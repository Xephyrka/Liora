package com.xephyrka.liora.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import com.xephyrka.liora.data.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import com.xephyrka.liora.ui.theme.LioraTheme
import androidx.core.net.toUri

/**
 * Screen displayed when a task reminder triggers.
 * It handles the alarm sound playback and provides action buttons to 
 * start, snooze, or mark the task as done.
 */
@Composable
fun NotificationScreen(
    taskTitle: String,
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    // Observed state for the user's custom notification sound
    val savedSoundUri by preferenceManager.notificationSoundFlow.collectAsState(initial = null)

    // Intercept back navigation to handle as dismissal
    BackHandler(onBack = onDismiss)

    // Alarm sound playback lifecycle
    DisposableEffect(savedSoundUri) {
        val soundUri = savedSoundUri?.toUri() ?: try {
            val resId = context.resources.getIdentifier("reminder_sound", "raw", context.packageName)
            if (resId != 0) {
                "android.resource://${context.packageName}/$resId".toUri()
            } else {
                Settings.System.DEFAULT_ALARM_ALERT_URI ?: Settings.System.DEFAULT_NOTIFICATION_URI
            }
        } catch (_: Exception) {
            Settings.System.DEFAULT_ALARM_ALERT_URI ?: Settings.System.DEFAULT_NOTIFICATION_URI
        }

        val player = MediaPlayer().apply {
            setDataSource(context, soundUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
        
        onDispose {
            player.stop()
            player.release()
        }
    }

    NotificationContent(
        taskTitle = taskTitle,
        onStart = onStart,
        onSnooze = onSnooze,
        onDone = onDone
    )
}

/**
 * The layout implementation for the NotificationScreen.
 * Displays the current time, task details, and action buttons.
 */
@Composable
fun NotificationContent(
    taskTitle: String,
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val currentTime = remember {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    val commonShape = MaterialTheme.shapes.medium

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PatternBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clock display
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = commonShape,
                modifier = Modifier.padding(bottom = 32.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            // Task title card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                shape = commonShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TIME TO GET UP",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            ActionButtons(
                onStart = onStart,
                onSnooze = onSnooze,
                onDone = onDone,
                shape = commonShape
            )
        }
    }
}

/**
 * Row containing primary and secondary action buttons for task interaction.
 */
@Composable
fun ActionButtons(
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary "Start" button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow, 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "I'm starting now!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Secondary "Done" and "Snooze" buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = shape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Already Done",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            FilledTonalButton(
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = shape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Snooze 10m",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Decorative background component featuring scattered circular shapes.
 */
@Composable
fun PatternBackground() {
    val patternColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = java.util.Random(42)
        repeat(12) {
            val center = Offset(
                x = random.nextFloat() * size.width,
                y = random.nextFloat() * size.height
            )
            val radius = (150 + random.nextFloat() * 250).dp.toPx()
            drawCircle(
                color = patternColor,
                radius = radius,
                center = center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    LioraTheme {
        NotificationContent(
            taskTitle = "Go do your tasks",
            onStart = {},
            onSnooze = {},
            onDone = {}
        )
    }
}
