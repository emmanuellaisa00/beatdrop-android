package com.beatdrop.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.beatdrop.app.data.local.SettingsStore

private val BeatDarkColors = darkColorScheme(
    primary = Color(0xFFFF375F),
    onPrimary = Color.White,
    background = Color(0xFF365475),
    onBackground = Color.White,
    surface = Color(0xFF050608),
    onSurface = Color.White,
)

private val BeatLightColors = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color.White,
    background = Color(0xFF5981B1),
    onBackground = Color(0xFF121316),
    surface = Color.White,
    onSurface = Color(0xFF121316),
)

@Composable
fun BeatDropTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val light = when (SettingsStore.appearanceMode) {
        SettingsStore.Appearance.SYSTEM -> !systemDark
        SettingsStore.Appearance.DARK -> false
        SettingsStore.Appearance.LIGHT -> true
    }
    BeatThemeController.isLight = light

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = light
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = light
        }
    }
    MaterialTheme(
        colorScheme = if (light) BeatLightColors else BeatDarkColors,
        typography = AppTypography,
        content = content
    )
}
