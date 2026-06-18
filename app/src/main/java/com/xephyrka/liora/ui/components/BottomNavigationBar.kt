package com.xephyrka.liora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.xephyrka.liora.ui.components.ShowcaseState
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xephyrka.liora.navigation.NavigationItem
import com.xephyrka.liora.navigation.Screen

/**
 * A custom translucent bottom navigation bar with square touch feedback.
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    showcaseState: ShowcaseState? = null
) {
    /** The list of navigation destinations to display in the bar. */
    val items = listOf(
        NavigationItem("Home", Icons.Default.Home, Screen.Home.route),
        NavigationItem("Settings", Icons.Default.Settings, Screen.Settings.route),
    )

    /** Observes the current navigation state to highlight the active tab. */
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    /** The route string of the screen currently being viewed. */
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .onGloballyPositioned { coords ->
                            if (item.route == Screen.Settings.route) {
                                showcaseState?.updateTargetCoordinates("settings_target", coords)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.icon else when(item.route) {
                            Screen.Home.route -> Icons.Outlined.Home
                            else -> Icons.Outlined.Settings
                        },
                        contentDescription = item.title,
                        modifier = Modifier.size(28.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Add a spacer between items to create dead space
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
