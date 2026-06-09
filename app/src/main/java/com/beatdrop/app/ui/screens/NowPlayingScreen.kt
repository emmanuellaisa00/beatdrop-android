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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
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
    debugLogText: String = "",
    onRetry: () -> Unit = {},
    onToggleLike: () -> Unit = {},
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs) else 0f
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDevices by remember { mutableStateOf(false) }
    val shareTrack: () -> Unit = {
        runCatching {
            context.startActivity(android.content.Intent.createChooser(
                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "${track.title} — ${track.artist}")
                }, "Share song"))
        }
    }

    Box(Modifier.fillMaxSize()) {
        NowPlayingBackground()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(610.dp)
            ) {
                // iOS-style pull handle
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .size(width = 58.dp, height = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x66FFFFFF))
                        .clickable(onClick = onClose)
                )

                // Large artwork/portrait stage
                CoverArt(
                    track.artworkUri,
                    track.colorKey,
                    size = null,
                    corner = 0.dp,
                    glyphSize = 128.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(610.dp)
                )

                // dark top + red bottom wash, like Apple Music artwork player
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0x99000000),
                                0.42f to Color(0x11000000),
                                0.70f to Color(0x66150000),
                                1f to Color(0xE0180708),
                            )
                        )
                )

                // top actions
                CircleButton(Icons.Rounded.ExpandMore, "Minimize", 40.dp, 20.dp, onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 18.dp))
                CircleButton(Icons.Rounded.MoreHoriz, "More", 40.dp, 18.dp, onClick = shareTrack, modifier = Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 18.dp))

                // title / artist over artwork bottom
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            track.title,
                            style = BeatType.LargeTitle.copy(fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = (-0.025f).em),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (isResolving) "Loading audio… ${track.artist}" else track.artist,
                            style = BeatType.CardSub.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                            color = if (isResolving) BeatColors.AccentEnd else Color(0xCCFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    AddButton(isLiked) { onToggleLike() }
                    CircleButton(Icons.Rounded.MoreHoriz, "Share", 42.dp, 18.dp, onClick = shareTrack)
                }
            }

            if (resolveError != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(resolveError, style = BeatType.TrackSub, color = BeatColors.AccentEnd, modifier = Modifier.weight(1f).padding(end = 12.dp), maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DebugPill("Copy logs") { clipboard.setText(AnnotatedString(debugLogText.ifBlank { "No online playback logs yet." })) }
                        DebugPill("Retry", onRetry)
                    }
                }
            } else if (isResolving && debugLogText.isNotBlank()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    DebugPill("Copy logs") { clipboard.setText(AnnotatedString(debugLogText)) }
                }
            }

            ProgressBar(
                progress = progress,
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = onSeek,
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 6.dp)
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CtrlButton(Icons.Rounded.SkipPrevious, "Previous", onClick = onPrevious, size = 54.dp)
                PlayRing(isPlaying && !isResolving, onTogglePlay)
                CtrlButton(Icons.Rounded.SkipNext, "Next", onClick = onNext, size = 54.dp)
            }

            VolumePill(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            Row(
                Modifier.fillMaxWidth().padding(start = 42.dp, end = 42.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomAction(Icons.Rounded.KeyboardArrowUp, "Lyrics", onClick = onOpenLyrics)
                BottomAction(Icons.Rounded.Speaker, "Devices", onClick = { showDevices = true })
                BottomAction(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", onClick = onOpenQueue)
            }

            Spacer(Modifier.weight(1f))
        }
    }

    if (showDevices) {
        DeviceRouteDialog(
            onDismiss = { showDevices = false },
            onOpenBluetooth = {
                runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
                showDevices = false
            },
            onOpenSound = {
                runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)) }
                showDevices = false
            },
            onOpenQueue = {
                showDevices = false
                onOpenQueue()
            }
        )
    }
}

@Composable
private fun DeviceRouteDialog(
    onDismiss: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101824),
        title = { Text("Devices", style = BeatType.SectionTitle, color = BeatColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a device route or jump into the active queue. BeatDrop uses Android’s system audio routing for reliable playback.", style = BeatType.CardSub, color = BeatColors.TextSecondary)
                DebugPill("Open Bluetooth devices", onOpenBluetooth)
                DebugPill("Open sound settings", onOpenSound)
                DebugPill("Open queue", onOpenQueue)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close", color = BeatColors.Accent) } }
    )
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
                    0f to Color(0xFF20262D),
                    0.34f to Color(0xFF202326),
                    0.62f to Color(0xFF3A1E1F),
                    1f to Color(0xFF210B0D),
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xCCB82420), Color.Transparent),
                    center = Offset(520f, 1180f),
                    radius = 980f * haze
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x55365475), Color.Transparent),
                    center = Offset(240f, 360f),
                    radius = 840f
                )
            )
    )
}

@Composable
private fun DebugPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x22FF375F))
            .border(1.dp, BeatColors.Accent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = BeatType.Pill, color = BeatColors.Accent)
    }
}

@Composable
private fun CircleButton(
    icon: ImageVector,
    desc: String,
    box: androidx.compose.ui.unit.Dp,
    glyph: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
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
private fun CtrlButton(icon: ImageVector, desc: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
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
private fun VolumePill(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Color(0x33100028))
            .border(3.dp, Color(0xFF7D2CFF), RoundedCornerShape(29.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Speaker, "Volume down", tint = Color(0xCCFFFFFF), modifier = Modifier.size(19.dp))
        Box(
            Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.70f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCCFFFFFF))
            )
        }
        Icon(Icons.Rounded.Speaker, "Volume up", tint = Color(0xE6FFFFFF), modifier = Modifier.size(23.dp))
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
