package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DlmsDarkColorScheme = darkColorScheme(
    primary = HighDensityLavender,
    onPrimary = HighDensityOnLavender,
    primaryContainer = HighDensityLavenderContainer,
    onPrimaryContainer = HighDensityLavender,
    secondary = HighDensityLavender,
    onSecondary = HighDensityOnLavender,
    secondaryContainer = HighDensityBorder,
    onSecondaryContainer = HighDensityLavender,
    tertiary = AudioAmber,
    onTertiary = HighDensityOnLavender,
    background = HighDensityBg,
    onBackground = HighDensityTextPrimary,
    surface = HighDensityCard,
    onSurface = HighDensityTextPrimary,
    surfaceVariant = HighDensityCardAlt,
    onSurfaceVariant = HighDensityTextSecondary,
    outline = HighDensityBorder,
    error = HighDensityMuteBg,
    onError = HighDensityMuteText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Keep consistent pro-audio rack theme across all Android devices
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DlmsDarkColorScheme,
        typography = Typography,
        content = content
    )
}
