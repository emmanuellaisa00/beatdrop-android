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
    Home(Color(0xFF365475), Color(0xFF20384F), Color(0xFF5981B1), Color(0xFFF8FBFF)),
    Search(Color(0xFF365475), Color(0xFF253F59), Color(0xFF5981B1), Color(0xFFF8FBFF)),
    Library(Color(0xFF365475), Color(0xFF20384F), Color(0xFF5981B1), Color(0xFFF8FBFF)),
    Add(Color(0xFF365475), Color(0xFF2D4661), Color(0xFF5981B1), Color(0xFFF8FBFF)),
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
                .background(BeatColors.Background)
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
                        0.45f to Color(0xFF2B4662),
                        0.88f to BeatColors.Background,
                    )
                )
        )
    }
}
