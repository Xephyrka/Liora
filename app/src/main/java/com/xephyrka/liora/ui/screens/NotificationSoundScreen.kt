package com.xephyrka.liora.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.xephyrka.liora.data.PreferenceManager
import com.xephyrka.liora.ui.components.CupertinoGroupedSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen that allows the user to select a custom notification sound for reminders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSoundScreen(
    navController: NavHostController,
    preferenceManager: PreferenceManager,
    currentSoundUri: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val systemSounds = remember {
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = manager.cursor
        val list = mutableListOf<Pair<String, String>>()
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position).toString()
            list.add(title to uri)
        }
        list
    }

    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun playSound(uri: String?) {
        activePlayer?.stop()
        activePlayer?.release()
        
        val soundUri = if (uri == null) {
            Settings.System.DEFAULT_NOTIFICATION_URI
        } else {
            uri.toUri()
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
            }
            activePlayer = player
            scope.launch {
                delay(3000)
                if (activePlayer == player) {
                    player.release()
                    activePlayer = null
                }
            }
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            activePlayer?.stop()
            activePlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notification Sound", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item {
                Text(
                    "SYSTEM DEFAULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
                CupertinoGroupedSection {
                    SoundItemRow(
                        title = "Default",
                        isSelected = currentSoundUri == null,
                        onClick = {
                            playSound(null)
                            scope.launch { preferenceManager.setNotificationSound(null) }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    "OTHER SOUNDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }

            item {
                CupertinoGroupedSection {
                    systemSounds.forEach { (title, uri) ->
                        SoundItemRow(
                            title = title,
                            isSelected = currentSoundUri == uri,
                            onClick = {
                                playSound(uri)
                                scope.launch { preferenceManager.setNotificationSound(uri) }
                            }
                        )
                        if (uri != systemSounds.last().second) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 48.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SoundItemRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Notifications,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
