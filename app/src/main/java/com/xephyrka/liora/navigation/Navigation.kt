package com.xephyrka.liora.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xephyrka.liora.data.PreferenceManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.xephyrka.liora.ui.screens.HomeScreen
import com.xephyrka.liora.ui.screens.SettingsScreen
import com.xephyrka.liora.ui.screens.NewTaskScreen
import com.xephyrka.liora.ui.screens.NotificationScreen
import com.xephyrka.liora.ui.components.BottomNavigationBar
import com.xephyrka.liora.ui.components.ShowcaseStep
import com.xephyrka.liora.ui.components.rememberShowcaseState
import com.xephyrka.liora.viewmodel.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xephyrka.liora.data.model.ItemType
import kotlinx.coroutines.launch

/**
 * Sealed class representing all navigate-able screens in the application.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewTask : Screen("new_task_screen/{listId}?taskId={taskId}&type={type}") {
        fun createRoute(listId: Int, taskId: Int? = null, type: ItemType? = null): String {
            var route = "new_task_screen/$listId"
            val queryParams = mutableListOf<String>()
            taskId?.let { queryParams.add("taskId=$it") }
            type?.let { queryParams.add("type=${it.name}") }
            
            if (queryParams.isNotEmpty()) {
                route += "?" + queryParams.joinToString("&")
            }
            return route
        }
    }
    object Settings : Screen("settings")
    object NotificationSound : Screen("notification_sound")
    object Notification : Screen("notification/{taskTitle}")
}

/**
 * The main UI coordinator that sets up navigation and global app state.
 */
@Composable
fun MainScreen(
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()
    
    val darkMode by preferenceManager.darkModeFlow.collectAsState(initial = false)
    val useSystemTheme by preferenceManager.useSystemThemeFlow.collectAsState(initial = true)
    val userName by preferenceManager.userNameFlow.collectAsState(initial = "")
    val notificationSound by preferenceManager.notificationSoundFlow.collectAsState(initial = null)
    val developerMode by preferenceManager.developerModeFlow.collectAsState(initial = false)
    val blockedApps by preferenceManager.blockedAppsFlow.collectAsState(initial = emptySet())
    val startHour by preferenceManager.blockingStartHourFlow.collectAsState(initial = 22)
    val endHour by preferenceManager.blockingEndHourFlow.collectAsState(initial = 10)
    val grayscaleEnabled by preferenceManager.grayscaleEnabledFlow.collectAsState(initial = false)
    val fullScreenEnabled by preferenceManager.fullScreenNotificationsEnabledFlow.collectAsState(initial = true)
    val onboardingShown by preferenceManager.onboardingShownFlow.collectAsState(initial = true)

    val showcaseState = rememberShowcaseState(
        steps = listOf(
            ShowcaseStep(
                id = "welcome", 
                title = "Welcome to Liora", 
                description = "Your companion for a focused digital life",
                isFullScreen = true
            ),
            ShowcaseStep(
                id = "focus_mode", 
                title = "Focus Mode", 
                description = "Block distracting apps and do with your tasks distraction-free",
                isFullScreen = true
            ),
            ShowcaseStep("home", "Your Dashboard", "Manage all your tasks and lists from one central place"),
            ShowcaseStep("tabs", "Task Lists", "Organize your life into custom categories", "tabs_target"),
            ShowcaseStep("add", "Create New", "Tap here to add your first task or note", "add_target"),
            ShowcaseStep("settings", "Configure", "Personalize your experience in settings", "settings_target")
        ),
        onFinish = {
            scope.launch {
                preferenceManager.setOnboardingShown(true)
                onRequestPermissions() // Ask for permissions after guide
            }
        }
    )

    LaunchedEffect(onboardingShown) {
        if (!onboardingShown) {
            showcaseState.reset()
        } else {
            onRequestPermissions()
        }
    }

    val navController = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if ((currentRoute == Screen.Home.route) || (currentRoute == Screen.Settings.route)) {
                BottomNavigationBar(
                    navController = navController,
                    showcaseState = showcaseState
                )
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(200)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(200)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(200)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(200)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(200)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    viewModel = taskViewModel,
                    externalPadding = paddingValues,
                    showcaseState = showcaseState
                )
            }

            composable(
                route = Screen.NewTask.route,
                arguments = listOf(
                    navArgument("listId") { type = NavType.IntType },
                    navArgument("taskId") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getInt("listId") ?: 1
                val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                val typeString = backStackEntry.arguments?.getString("type")
                val initialType = if (typeString != null) {
                    try { ItemType.valueOf(typeString) } catch(_: Exception) { ItemType.TASK }
                } else {
                    ItemType.TASK
                }

                NewTaskScreen(
                    navController = navController,
                    viewModel = taskViewModel,
                    listId = listId,
                    taskId = taskId,
                    initialType = initialType
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    externalPadding = paddingValues,
                    viewModel = taskViewModel,
                    preferenceManager = preferenceManager,
                    darkMode = darkMode,
                    useSystemTheme = useSystemTheme,
                    userName = userName,
                    notificationSound = notificationSound,
                    developerMode = developerMode,
                    blockedApps = blockedApps,
                    startHour = startHour,
                    endHour = endHour,
                    grayscaleEnabled = grayscaleEnabled,
                    fullScreenEnabled = fullScreenEnabled,
                    onNotificationSoundClick = {
                        navController.navigate(Screen.NotificationSound.route)
                    }
                )
            }

            composable(Screen.NotificationSound.route) {
                com.xephyrka.liora.ui.screens.NotificationSoundScreen(
                    navController = navController,
                    preferenceManager = preferenceManager,
                    currentSoundUri = notificationSound
                )
            }

            composable(
                route = Screen.Notification.route,
                arguments = listOf(
                    navArgument("taskTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taskTitle = backStackEntry.arguments?.getString("taskTitle") ?: ""
                NotificationScreen(
                    taskTitle = taskTitle,
                    onStart = { navController.popBackStack() },
                    onSnooze = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
        
        com.xephyrka.liora.ui.components.Showcase(state = showcaseState)
    }
}

/**
 * Data class representing an item in the bottom navigation bar.
 */
data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
