package com.beatdrop.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.components.CoverArt
import com.beatdrop.app.ui.theme.AccentBrush
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/** Now Playing — fullscreen, ported 1:1 from #screen-now / .np-* in the HTML. */
@Composable
fun NowPlayingScreen(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    isLiked: Boolean = false,
    isResolving: Boolean = false,
    resolveError: String? = null,
    onRetry: () -> Unit = {},
    onToggleLike: () -> Unit = {},
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs) else 0f

    Box(Modifier.fillMaxSize()) {
        NowPlayingBackground()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top: chevron-down | centered album name | more ──
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp)
            ) {
                CircleButton(Icons.Rounded.ExpandMore, "Minimize", 40.dp, 20.dp, onClick = onClose)
                Text(
                    track.album.uppercase(),
                    style = BeatType.CardSub.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.24f.em
                    ),
                    color = Color(0xD9FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp)
                )
                Box(Modifier.align(Alignment.CenterEnd)) {
                    CircleButton(Icons.Rounded.MoreHoriz, "More", 40.dp, 18.dp) {}
                }
            }

            // ── Cover (1:1, radius 20, margin 4/20) ──
            Box(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                CoverArt(
                    track.artworkUri, track.colorKey,
                    size = null, corner = 20.dp, glyphSize = 96.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                )
            }

            // ── Track meta: title 28/900, artist 14/600@.58, + button 38 ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        track.title,
                        style = BeatType.LargeTitle.copy(
                            fontSize = 28.sp,
                            letterSpacing = (-0.030f).em
                        ),
                        color = BeatColors.TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isResolving) "Loading audio… ${track.artist}" else track.artist,
                        style = BeatType.CardSub.copy(
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        color = if (isResolving) BeatColors.Accent else Color(0x94FFFFFF),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                AddButton(isLiked) { onToggleLike() }
            }

            if (resolveError != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        resolveError,
                        style = BeatType.TrackSub,
                        color = BeatColors.Accent,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        maxLines = 2,
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x22FF375F))
                            .border(1.dp, BeatColors.Accent, RoundedCornerShape(18.dp))
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Retry", style = BeatType.Pill, color = BeatColors.Accent)
                    }
                }
            }

            // ── Progress (5dp bar, 16dp knob, draggable) ──
            ProgressBar(
                progress = progress,
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = onSeek,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)
            )

            // ── Transport: prev | play-ring(74) | next, gap 50 ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(50.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CtrlButton(Icons.Rounded.SkipPrevious, "Previous", onClick = onPrevious)
                PlayRing(isPlaying && !isResolving, onTogglePlay)
                CtrlButton(Icons.Rounded.SkipNext, "Next", onClick = onNext)
            }

            // ── Bottom actions: devices | share ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BottomAction(Icons.Rounded.Speaker, "Devices")
                BottomAction(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", onClick = onOpenQueue)
                BottomAction(Icons.Rounded.IosShare, "Share")
            }

            Spacer(Modifier.weight(1f))

            // ── Lyrics drawer (84dp, rounded top 28) ──
            LyricsDrawer(onOpenLyrics)
        }
    }
}

@Composable
private fun NowPlayingBackground() {
    val transition = rememberInfiniteTransition(label = "npbg")
    val haze by transition.animateFloat(
        1f, 1.06f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "haze"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF0F2640),
                    0.5f to Color(0xFF0A1828),
                    1f to Color(0xFF040A12),
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF22405E), Color.Transparent),
                    center = Offset.Unspecified,
                    radius = 1100f
                )
            )
    )
}

@Composable
private fun CircleButton(icon: ImageVector, desc: String, box: androidx.compose.ui.unit.Dp, glyph: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(box)
            .clip(CircleShape)
            .background(Color(0x52000000))
            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(glyph))
    }
}

@Composable
private fun AddButton(added: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (added) Color(0x38FF375F) else Color(0x0FFFFFFF))
            .border(1.dp, if (added) Color(0x80FF375F) else Color(0x29FFFFFF), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (added) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            "Like",
            tint = if (added) BeatColors.Accent else Color(0xEBFFFFFF),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableStateOf(1) }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0 && widthPx > 0) {
                            onSeek((offset.x / widthPx * durationMs).toLong())
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // track
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x24FFFFFF))
            )
            // fill
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AccentBrush)
            )
            // knob (16dp white)
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color(0x26FFFFFF), CircleShape)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(positionMs.formatTime(), style = BeatType.TrackSub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Color(0x7AFFFFFF))
            Text(durationMs.formatTime(), style = BeatType.TrackSub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Color(0x7AFFFFFF))
        }
    }
}

@Composable
private fun CtrlButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color(0xF2FFFFFF), modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun PlayRing(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(Color(0x0FFFFFFF))
            .border(1.5.dp, Color(0x52FFFFFF), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun BottomAction(icon: ImageVector, desc: String, onClick: () -> Unit = {}) {
    Box(
        Modifier
            .size(34.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color(0xCCFFFFFF), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun LyricsDrawer(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xD10E1E32))
            .border(
                1.dp, Color(0x1FFFFFFF),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.KeyboardArrowUp, "Lyrics", tint = Color(0x99FFFFFF), modifier = Modifier.size(24.dp))
        Text(
            "Lyrics",
            style = BeatType.TopBarTitle.copy(fontSize = 17.sp),
            color = BeatColors.TextPrimary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
