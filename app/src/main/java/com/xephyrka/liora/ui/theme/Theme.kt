package com.xephyrka.liora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cupertino-inspired light color palette
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = CardBackground,
    secondary = AccentIndigo,
    background = Background,
    onBackground = PrimaryText,
    surface = CardBackground,
    onSurface = PrimaryText,
    surfaceVariant = Background,
    onSurfaceVariant = SecondaryText,
    outline = DividerColor
)

// Cupertino-inspired dark color palette
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = CardBackground,
    secondary = AccentCyan,
    background = Color(0xFF000000), // Pure black for OLED
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E), // iOS Dark Gray
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0xFF3A3A3C),
    error = Color(0xFFFF453A) // Saturated iOS Red for Dark Mode
)

@Composable
fun LioraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We ignore dynamicColor to enforce our Cupertino palette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
