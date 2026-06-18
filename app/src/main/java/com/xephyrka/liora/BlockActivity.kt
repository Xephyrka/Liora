package com.xephyrka.liora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xephyrka.liora.ui.theme.LioraTheme
import com.xephyrka.liora.service.AppBlockerService

/**
 * Activity that displays a blocking screen when a user attempts to open a restricted application.
 * It provides a visual intervention to help users stay focused during their scheduled blocking hours.
 */
class BlockActivity : ComponentActivity() {
    /**
     * Called when the activity is launched by the [AppBlockerService].
     * Extracts the package name of the blocked app and sets up the Composable UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        /** The package identifier of the application that was blocked. */
        val blockedPackage = intent.getStringExtra("blocked_package") ?: "Unknown App"

        setContent {
            LioraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BlockScreen(blockedPackageName = blockedPackage) {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                }
            }
        }
    }
}

/**
 * The UI content for the blocking screen.
 * Displays a focus-oriented message and a call to action to return to productive tasks.
 */
@Composable
fun BlockScreen(
    /** The package name or label of the application being blocked. */
    blockedPackageName: String, 
    /** Callback triggered when the user clicks the exit button. */
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Focus Mode Active",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You've blocked $blockedPackageName to stay productive. Focus on your tasks!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Productivity")
        }
    }
}
