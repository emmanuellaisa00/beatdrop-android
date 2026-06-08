package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.CoverPalette
import com.beatdrop.app.ui.theme.brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote

/**
 * Album/track cover. Uses real artwork if present, else the gradient palette
 * fallback (the look from the HTML mock), with the subtle top-left highlight overlay.
 */
@Composable
fun CoverArt(
    artworkUri: android.net.Uri?,
    colorKey: String?,
    size: Dp? = null,
    corner: Dp = 12.dp,
    icon: ImageVector = Icons.Rounded.MusicNote,
    glyphSize: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val palette = CoverPalette.from(colorKey)
    val fallbackBrush = Brush.linearGradient(
        listOf(Color(0xFF202027), Color(0xFF111116), Color(0xFF050507))
    )
    Box(
        modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(RoundedCornerShape(corner))
            .background(if (artworkUri == null) fallbackBrush else palette.brush())
            .border(1.dp, BeatColors.GlassBorder, RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center
    ) {
        val glyph = size?.times(0.38f) ?: glyphSize
        if (artworkUri != null) {
            SubcomposeAsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { GradientGlyph(icon, glyph) },
                loading = { GradientGlyph(icon, glyph) },
            )
        } else {
            GradientGlyph(icon, glyph)
        }
        // top-left highlight overlay (matches .art::after)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        0f to Color(0x24FFFFFF),
                        0.55f to Color.Transparent,
                    )
                )
        )
    }
}

@Composable
private fun GradientGlyph(icon: ImageVector, glyph: Dp) {
    Icon(
        icon,
        contentDescription = null,
        tint = Color(0x4DFFFFFF),
        modifier = Modifier.size(glyph)
    )
}

fun Track.coverIcon(): ImageVector = Icons.Rounded.MusicNote
