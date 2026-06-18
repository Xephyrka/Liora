package com.xephyrka.liora.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Extension property to provide a DataStore instance for the application context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages the persistence and retrieval of user preferences using Jetpack DataStore.
 * Provides a reactive API via Kotlin Flows for UI observation.
 */
class PreferenceManager(private val context: Context) {

    /** Keys for storing various user settings in DataStore. */
    companion object {
        /** Key for the manual dark mode setting. */
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        /** Key for enabling/disabling Material You dynamic colors. */
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        /** Key for determining if the app should follow the system theme. */
        val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        /** Key for the user's display name. */
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        /** Key for the URI of the custom notification sound. */
        val NOTIFICATION_SOUND_KEY = stringPreferencesKey("notification_sound")
        /** Key for the set of package names restricted during blocking hours. */
        val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_apps")
        /** Key for the start hour (0-23) of the focus/blocking period. */
        val BLOCKING_START_HOUR_KEY = intPreferencesKey("blocking_start_hour")
        /** Key for the end hour (0-23) of the focus/blocking period. */
        val BLOCKING_END_HOUR_KEY = intPreferencesKey("blocking_end_hour")
        /** Key for enabling automatic grayscale on blocked apps. */
        val GRAYSCALE_ENABLED_KEY = booleanPreferencesKey("grayscale_enabled")
        /** Key for enabling full-screen notification activity for reminders. */
        val FULL_SCREEN_NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("full_screen_notifications_enabled")
        /** Key for enabling developer tools in the settings menu. */
        val DEVELOPER_MODE_KEY = booleanPreferencesKey("developer_mode")
        /** Key for tracking if onboarding has been shown. */
        val ONBOARDING_SHOWN_KEY = booleanPreferencesKey("onboarding_shown")
    }

    /** Flow emitting the current dark mode preference. Defaults to false. */
    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    /** Flow emitting the current dynamic color preference. Defaults to true. */
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }

    /** Flow emitting whether the app follows the system theme. Defaults to true. */
    val useSystemThemeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_SYSTEM_THEME_KEY] ?: true
    }

    /** Flow emitting the user's saved name. Defaults to an empty string. */
    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: ""
    }

    /** Flow emitting the URI of the selected notification sound. Null means default system sound. */
    val notificationSoundFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_SOUND_KEY]
    }

    /** Flow emitting the set of package names currently being blocked. */
    val blockedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[BLOCKED_APPS_KEY] ?: emptySet()
    }

    /** Flow emitting the start time of the blocking window. Defaults to 10 PM (22). */
    val blockingStartHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BLOCKING_START_HOUR_KEY] ?: 22
    }

    /** Flow emitting the end time of the blocking window. Defaults to 10 AM (10). */
    val blockingEndHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BLOCKING_END_HOUR_KEY] ?: 10
    }

    /** Flow emitting whether grayscale mode is enabled for restricted apps. */
    val grayscaleEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GRAYSCALE_ENABLED_KEY] ?: false
    }

    /** Flow emitting whether reminders should use the full-screen activity. */
    val fullScreenNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FULL_SCREEN_NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    /** Flow emitting whether developer mode is enabled. */
    val developerModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVELOPER_MODE_KEY] ?: false
    }

    /** Flow emitting whether onboarding has been shown. */
    val onboardingShownFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_SHOWN_KEY] ?: false
    }

    /** Updates the dark mode preference. */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    /** Updates the preference for following the system theme. */
    suspend fun setUseSystemTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] = enabled
        }
    }

    /** Updates the saved user name. */
    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    /** Updates the custom notification sound URI. */
    suspend fun setNotificationSound(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(NOTIFICATION_SOUND_KEY)
            } else {
                preferences[NOTIFICATION_SOUND_KEY] = uri
            }
        }
    }

    /** Updates the list of applications to be blocked. */
    suspend fun setBlockedApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_APPS_KEY] = apps
        }
    }

    /** Enables or disables the automatic grayscale feature. */
    suspend fun setGrayscaleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GRAYSCALE_ENABLED_KEY] = enabled
        }
    }

    /** Updates the full-screen notification preference. */
    suspend fun setFullScreenNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FULL_SCREEN_NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    /** Enables or disables developer mode. */
    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MODE_KEY] = enabled
        }
    }

    /** Updates the blocking window hours. */
    suspend fun setBlockingHours(start: Int, end: Int) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKING_START_HOUR_KEY] = start
            preferences[BLOCKING_END_HOUR_KEY] = end
        }
    }

    /** Updates the onboarding shown status. */
    suspend fun setOnboardingShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_SHOWN_KEY] = shown
        }
    }

    /** Clears all saved preferences, resetting the app to default settings. */
    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
