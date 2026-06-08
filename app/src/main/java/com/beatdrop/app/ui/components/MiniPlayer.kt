package com.beatdrop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.theme.AccentBrush
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatThemeController
import com.beatdrop.app.ui.theme.BeatType

/**
 * Mini player — ported from .mini in beatdrop-premium.html.
 * left/right 10, height 66, radius 20, glass bg, 50 art (r12),
 * title 14/700, artist 12/500@.60, cast+play 38 circles, 2.5px accent progress.
 */
@Composable
fun MiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = track != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        if (track == null) return@AnimatedVisibility
        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(20.dp))
                // base + top-left highlight overlay (matches the layered backgrounds)
                .background(BeatColors.MiniBg)
                .background(
                    Brush.linearGradient(
                        0f to if (BeatThemeController.isLight) Color(0x66FFFFFF) else Color(0x14FFFFFF),
                        0.5f to Color.Transparent,
                    )
                )
                .border(1.dp, BeatColors.GlassBorder, RoundedCornerShape(20.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                )
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 7.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(track.artworkUri, track.colorKey, size = 50.dp, corner = 12.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        track.title,
                        style = BeatType.CardSub.copy(
                            fontSize = BeatType.CardTitle.fontSize,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = BeatColors.TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        style = BeatType.CardSub,
                        color = BeatColors.TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                IconCircle(Icons.Rounded.Cast, "Cast", tint = BeatColors.TextSecondary) { /* devices */ }
                IconCircle(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    onClick = onTogglePlay
                )
            }
            // 2.5px progress at the very bottom
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0x14FFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.5.dp)
                        .background(AccentBrush)
                )
            }
        }
    }
}

@Composable
private fun IconCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(22.dp))
    }
}
