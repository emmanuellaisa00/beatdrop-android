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
import com.beatdrop.app.ui.theme.BeatThemeController

/** Per-screen background. Light mode uses Apple-clean warm paper + pastel wash. */
enum class ScreenTheme(val top: Color, val mid: Color, val lightTop: Color, val lightMid: Color) {
    Home(Color(0xFF1A3D50), Color(0xFF0F1C23), Color(0xFFE8F6EE), Color(0xFFFFFBF4)),
    Search(Color(0xFF281D50), Color(0xFF0F0E1E), Color(0xFFEDE8FF), Color(0xFFFFFBF4)),
    Library(Color(0xFF5A2238), Color(0xFF1E0F1A), Color(0xFFE7F5FF), Color(0xFFFFFBF4)),
    Add(Color(0xFF30183E), Color(0xFF14101C), Color(0xFFFFEDD9), Color(0xFFFFFBF4)),
}

@Composable
fun ScreenBackground(theme: ScreenTheme, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "bgScale"
    )
    if (BeatThemeController.isLight) {
        Box(
            modifier
                .fillMaxSize()
                .background(BeatColors.Background)
                .background(
                    Brush.radialGradient(
                        colors = listOf(theme.lightTop, Color.Transparent),
                        center = Offset(240f, 0f),
                        radius = 950f,
                    )
                )
                .background(
                    Brush.verticalGradient(
                        0f to theme.lightMid,
                        0.52f to BeatColors.Background,
                        1f to Color(0xFFF0EDE6),
                    )
                )
        )
    } else {
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
}
