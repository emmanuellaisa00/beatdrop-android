package com.beatdrop.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens ported 1:1 from beatdrop-premium.html (:root variables + palette).
 */
object BeatColors {
    val Accent = Color(0xFFFF375F)
    val AccentEnd = Color(0xFFFF7A94)      // gradient partner used in .accent / fills
    val AccentDeep = Color(0xFFD01E43)     // .pill.active end
    val AccentDim = Color(0x59FF375F)      // rgba(255,55,95,0.35)
    val AccentGlow = Color(0x8CFF375F)     // rgba(255,55,95,0.55)

    val Background = Color(0xFF050608)
    val PhoneInk = Color(0xFF08060E)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x94FFFFFF)  // ~0.58
    val TextTertiary = Color(0x7AFFFFFF)   // ~0.48
    val TextMuted = Color(0x73FFFFFF)      // ~0.45
    val TextFaint = Color(0x61FFFFFF)      // ~0.38

    // Surfaces (glass)
    val Surface = Color(0x12FFFFFF)        // rgba(255,255,255,0.07)
    val SurfaceHover = Color(0x1CFFFFFF)   // rgba(255,255,255,0.11)
    val GlassBorder = Color(0x17FFFFFF)    // rgba(255,255,255,0.09)
    val DockBg = Color(0xCC0E0C12)         // rgba(14,12,18,0.80)
    val MiniBg = Color(0xE0140C12)         // rgba(20,12,18,0.88)
}

/**
 * Album cover gradient palette c-1 .. c-8 (linear-gradient 145deg).
 */
enum class CoverPalette(val colors: List<Color>) {
    C1(listOf(Color(0xFFFF7788), Color(0xFFC73560), Color(0xFF4A1530))),
    C2(listOf(Color(0xFF2A80B0), Color(0xFF0152A0), Color(0xFF01253E))),
    C3(listOf(Color(0xFFFFC55A), Color(0xFFD08520), Color(0xFF4A2C0A))),
    C4(listOf(Color(0xFFC09AFF), Color(0xFF7550E2), Color(0xFF2C1A5E))),
    C5(listOf(Color(0xFF25D278), Color(0xFF0F9855), Color(0xFF064E2A))),
    C6(listOf(Color(0xFFFF3D8E), Color(0xFF7A0FC4), Color(0xFF2C0458))),
    C7(listOf(Color(0xFF10DEA8), Color(0xFF1598C0), Color(0xFF073E50))),
    C8(listOf(Color(0xFFF25A7A), Color(0xFFFFCF70), Color(0xFFF88C6E)));

    companion object {
        fun from(key: String?): CoverPalette = when (key) {
            "c-1", "cover-liked" -> C1
            "c-2" -> C2
            "c-3" -> C3
            "c-4" -> C4
            "c-5" -> C5
            "c-6" -> C6
            "c-7" -> C7
            "c-8" -> C8
            else -> {
                // stable hash so the same id always maps to the same color
                val all = entries
                val h = (key?.hashCode() ?: 0)
                all[(h % all.size + all.size) % all.size]
            }
        }
    }
}

/** 145deg linear gradient brush from a 3-stop palette (matches CSS 0% / 55% / 100%). */
fun CoverPalette.brush(): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0f to colors[0],
        0.55f to colors[1],
        1f to colors[2],
    ),
    // 145deg ≈ down-right diagonal
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(1000f, 1400f),
)

/** Browse-tile palette bt-1..bt-8 (2-stop linear-gradient 145deg). */
enum class BrowsePalette(val a: Color, val b: Color) {
    BT1(Color(0xFFFF7A8A), Color(0xFFC73560)),
    BT2(Color(0xFF2A80B0), Color(0xFF0152A0)),
    BT3(Color(0xFF22D076), Color(0xFF0F9855)),
    BT4(Color(0xFFC09AFF), Color(0xFF7550E2)),
    BT5(Color(0xFFFFC55A), Color(0xFFD08520)),
    BT6(Color(0xFFFF3D8E), Color(0xFF7A0FC4)),
    BT7(Color(0xFF10DEA8), Color(0xFF1598C0)),
    BT8(Color(0xFFF25A7A), Color(0xFFFFCF70));

    fun brush(): Brush = Brush.linearGradient(
        colors = listOf(a, b),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(600f, 800f),
    )
}

val AccentBrush = Brush.linearGradient(
    listOf(BeatColors.Accent, BeatColors.AccentEnd)
)
val PillActiveBrush = Brush.linearGradient(
    listOf(BeatColors.Accent, BeatColors.AccentDeep)
)
