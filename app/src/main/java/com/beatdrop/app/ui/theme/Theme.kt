package com.beatdrop.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BeatDarkColors = darkColorScheme(
    primary = BeatColors.Accent,
    onPrimary = BeatColors.TextPrimary,
    background = BeatColors.Background,
    onBackground = BeatColors.TextPrimary,
    surface = BeatColors.Background,
    onSurface = BeatColors.TextPrimary,
)

@Composable
fun BeatDropTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = BeatDarkColors,
        typography = AppTypography,
        content = content
    )
}
