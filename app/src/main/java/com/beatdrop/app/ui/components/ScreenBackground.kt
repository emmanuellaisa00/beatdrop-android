package com.beatdrop.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Per-screen tinted, slowly "breathing" gradient background (matches .theme-* .bg). */
enum class ScreenTheme(val top: Color, val mid: Color) {
    Home(Color(0xFF1A3D50), Color(0xFF0F1C23)),
    Search(Color(0xFF281D50), Color(0xFF0F0E1E)),
    Library(Color(0xFF5A2238), Color(0xFF1E0F1A)),
    Add(Color(0xFF30183E), Color(0xFF14101C)),
}

@Composable
fun ScreenBackground(theme: ScreenTheme, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "bgScale"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF050608))
            .scale(scale)
            .background(
                Brush.radialGradient(
                    colors = listOf(theme.top, Color.Transparent),
                    center = Offset.Unspecified,
                    radius = 1400f
                )
            )
            .background(
                Brush.verticalGradient(
                    0f to theme.mid,
                    0.45f to Color(0xFF07090D),
                    0.78f to Color(0xFF000000),
                )
            )
    )
}
