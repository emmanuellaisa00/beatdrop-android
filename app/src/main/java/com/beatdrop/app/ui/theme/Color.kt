package com.beatdrop.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Dark-only iOS 26 Liquid Glass design tokens.
 * No light mode branches: the app is intentionally premium dark everywhere.
 */
object BeatColors {
    // iOS-like action blue; likes/errors remain warm red where semantically needed.
    val Accent = Color(0xFF0A84FF)
    val AccentEnd = Color(0xFF64D2FF)
    val AccentDeep = Color(0xFF0057D9)
    val AccentDim = Color(0x330A84FF)
    val AccentGlow = Color(0x660A84FF)

    val Like = Color(0xFFFF375F)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)

    // Requested premium dark foundation.
    val Background = Color(0xFF365475)
    val BackgroundDeep = Color(0xFF13263A)
    val BackgroundInk = Color(0xFF07111D)
    val PhoneInk = Color(0xFF0B1522)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xCCFFFFFF)
    val TextTertiary = Color(0x99FFFFFF)
    val TextMuted = Color(0x73FFFFFF)
    val TextFaint = Color(0x55FFFFFF)

    // Opaque-enough liquid glass; content should not bleed under player/dock.
    val Surface = Color(0x1FFFFFFF)
    val SurfaceHover = Color(0x2BFFFFFF)
    val Glass = Color(0xB3182636)
    val GlassStrong = Color(0xE0111D2B)
    val GlassBorder = Color(0x2EFFFFFF)
    val DockBg = Color(0xF20B1522)
    val MiniBg = Color(0xF2142231)
}

enum class CoverPalette(val colors: List<Color>) {
    C1(listOf(Color(0xFFFF7A8A), Color(0xFFD92C5C), Color(0xFF401326))),
    C2(listOf(Color(0xFF64D2FF), Color(0xFF0A84FF), Color(0xFF102D5C))),
    C3(listOf(Color(0xFFFFD166), Color(0xFFFF9F0A), Color(0xFF4A2C0A))),
    C4(listOf(Color(0xFFBF9BFF), Color(0xFF6E5BFF), Color(0xFF21185A))),
    C5(listOf(Color(0xFF30D158), Color(0xFF168A3A), Color(0xFF063B22))),
    C6(listOf(Color(0xFFFF375F), Color(0xFFBF5AF2), Color(0xFF2D0B52))),
    C7(listOf(Color(0xFF5EFFF0), Color(0xFF0A84FF), Color(0xFF06324B))),
    C8(listOf(Color(0xFFFF8A65), Color(0xFFFFD166), Color(0xFF7A2E12)));

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
                val all = entries
                val h = (key?.hashCode() ?: 0)
                all[(h % all.size + all.size) % all.size]
            }
        }
    }
}

fun CoverPalette.brush(): Brush = Brush.linearGradient(
    colorStops = arrayOf(0f to colors[0], 0.55f to colors[1], 1f to colors[2]),
    start = Offset(0f, 0f),
    end = Offset(1000f, 1400f),
)

enum class BrowsePalette(val a: Color, val b: Color) {
    BT1(Color(0xFFFF7A8A), Color(0xFFD92C5C)),
    BT2(Color(0xFF64D2FF), Color(0xFF0A84FF)),
    BT3(Color(0xFF30D158), Color(0xFF168A3A)),
    BT4(Color(0xFFBF9BFF), Color(0xFF6E5BFF)),
    BT5(Color(0xFFFFD166), Color(0xFFFF9F0A)),
    BT6(Color(0xFFFF375F), Color(0xFFBF5AF2)),
    BT7(Color(0xFF5EFFF0), Color(0xFF0A84FF)),
    BT8(Color(0xFFFF8A65), Color(0xFFFFD166));

    fun brush(): Brush = Brush.linearGradient(
        colors = listOf(a, b),
        start = Offset(0f, 0f),
        end = Offset(600f, 800f),
    )
}

val AccentBrush: Brush get() = Brush.linearGradient(listOf(BeatColors.Accent, BeatColors.AccentEnd))
val PillActiveBrush: Brush get() = Brush.linearGradient(listOf(BeatColors.Accent, BeatColors.AccentDeep))
val LiquidGlassBrush: Brush get() = Brush.linearGradient(
    listOf(Color(0x44FFFFFF), Color(0x0FFFFFFF), Color(0x220A84FF))
)
