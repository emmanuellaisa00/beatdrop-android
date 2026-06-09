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
import com.beatdrop.app.ui.theme.BeatColors

/** Dark-only iOS 26 Liquid Glass screen backdrops. */
enum class ScreenTheme(val glow: Color, val mid: Color) {
    Home(Color(0xFF4D6D94), Color(0xFF243E58)),
    Search(Color(0xFF4A658F), Color(0xFF253F59)),
    Library(Color(0xFF4A6B88), Color(0xFF21394F)),
    Add(Color(0xFF5A668B), Color(0xFF2D4661)),
}

@Composable
fun ScreenBackground(theme: ScreenTheme, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(12000), RepeatMode.Reverse),
        label = "bgScale"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(BeatColors.Background)
            .scale(scale)
            .background(
                Brush.radialGradient(
                    colors = listOf(theme.glow.copy(alpha = 0.95f), Color.Transparent),
                    center = Offset(280f, -80f),
                    radius = 1250f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(BeatColors.Accent.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(900f, 1300f),
                    radius = 1200f
                )
            )
            .background(
                Brush.verticalGradient(
                    0f to theme.mid.copy(alpha = 0.68f),
                    0.52f to BeatColors.BackgroundDeep.copy(alpha = 0.76f),
                    1f to BeatColors.BackgroundInk,
                )
            )
    )
}
