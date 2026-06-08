package com.beatdrop.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Runtime theme switch read by the color tokens below. */
object BeatThemeController {
    var isLight by mutableStateOf(false)
}

/**
 * Design tokens. Dark mode keeps the original BeatDrop glass/pink look.
 * Light mode is intentionally different: Apple-clean warm surfaces with a
 * Spotify-green accent.
 */
object BeatColors {
    val Accent get() = if (BeatThemeController.isLight) Color(0xFF1DB954) else Color(0xFFFF375F)
    val AccentEnd get() = if (BeatThemeController.isLight) Color(0xFF58D68D) else Color(0xFFFF7A94)
    val AccentDeep get() = if (BeatThemeController.isLight) Color(0xFF0E8F43) else Color(0xFFD01E43)
    val AccentDim get() = if (BeatThemeController.isLight) Color(0x331DB954) else Color(0x59FF375F)
    val AccentGlow get() = if (BeatThemeController.isLight) Color(0x551DB954) else Color(0x8CFF375F)

    val Background get() = if (BeatThemeController.isLight) Color(0xFFF7F5EF) else Color(0xFF050608)
    val PhoneInk get() = if (BeatThemeController.isLight) Color(0xFFFFFCF7) else Color(0xFF08060E)

    val TextPrimary get() = if (BeatThemeController.isLight) Color(0xFF121316) else Color(0xFFFFFFFF)
    val TextSecondary get() = if (BeatThemeController.isLight) Color(0xB3121316) else Color(0x94FFFFFF)
    val TextTertiary get() = if (BeatThemeController.isLight) Color(0x8C121316) else Color(0x7AFFFFFF)
    val TextMuted get() = if (BeatThemeController.isLight) Color(0x73121316) else Color(0x73FFFFFF)
    val TextFaint get() = if (BeatThemeController.isLight) Color(0x55121316) else Color(0x61FFFFFF)

    val Surface get() = if (BeatThemeController.isLight) Color(0xFFFFFFFF) else Color(0x12FFFFFF)
    val SurfaceHover get() = if (BeatThemeController.isLight) Color(0xFFF0EEE8) else Color(0x1CFFFFFF)
    val GlassBorder get() = if (BeatThemeController.isLight) Color(0x16000000) else Color(0x17FFFFFF)
    val DockBg get() = if (BeatThemeController.isLight) Color(0xF2FFFFFF) else Color(0xCC0E0C12)
    val MiniBg get() = if (BeatThemeController.isLight) Color(0xFFFFFFFF) else Color(0xE0140C12)
}

/** Album cover gradient palette c-1 .. c-8. */
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
                val all = entries
                val h = (key?.hashCode() ?: 0)
                all[(h % all.size + all.size) % all.size]
            }
        }
    }
}

fun CoverPalette.brush(): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0f to colors[0],
        0.55f to colors[1],
        1f to colors[2],
    ),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(1000f, 1400f),
)

/** Browse tile palette. Light mode uses softer Apple-clean pastels. */
enum class BrowsePalette(val a: Color, val b: Color) {
    BT1(Color(0xFFFFA7B2), Color(0xFFFFD8DE)),
    BT2(Color(0xFF9DD7FF), Color(0xFFDCEEFF)),
    BT3(Color(0xFF7DE0A4), Color(0xFFDDF8E6)),
    BT4(Color(0xFFC8B5FF), Color(0xFFECE6FF)),
    BT5(Color(0xFFFFD276), Color(0xFFFFF0C7)),
    BT6(Color(0xFFFF9DCD), Color(0xFFFFE0F0)),
    BT7(Color(0xFF76E4D0), Color(0xFFDDF7F2)),
    BT8(Color(0xFFFFB08F), Color(0xFFFFE1D2));

    fun brush(): Brush = Brush.linearGradient(
        colors = if (BeatThemeController.isLight) listOf(a, b) else when (this) {
            BT1 -> listOf(Color(0xFFFF7A8A), Color(0xFFC73560))
            BT2 -> listOf(Color(0xFF2A80B0), Color(0xFF0152A0))
            BT3 -> listOf(Color(0xFF22D076), Color(0xFF0F9855))
            BT4 -> listOf(Color(0xFFC09AFF), Color(0xFF7550E2))
            BT5 -> listOf(Color(0xFFFFC55A), Color(0xFFD08520))
            BT6 -> listOf(Color(0xFFFF3D8E), Color(0xFF7A0FC4))
            BT7 -> listOf(Color(0xFF10DEA8), Color(0xFF1598C0))
            BT8 -> listOf(Color(0xFFF25A7A), Color(0xFFFFCF70))
        },
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(600f, 800f),
    )
}

val AccentBrush: Brush get() = Brush.linearGradient(listOf(BeatColors.Accent, BeatColors.AccentEnd))
val PillActiveBrush: Brush get() = Brush.linearGradient(listOf(BeatColors.Accent, BeatColors.AccentDeep))
