package com.beatdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.LrcLibProvider
import com.beatdrop.app.data.online.LrcParser
import com.beatdrop.app.data.online.LyricLine
import com.beatdrop.app.ui.components.CoverArt
import com.beatdrop.app.ui.theme.AccentBrush
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/**
 * Expanded Lyrics — visual design from #screen-lyrics, now backed by REAL synced
 * lyrics from LRCLIB (no API key). The active line highlights and auto-scrolls to
 * the playback position; tap a line to seek. Falls back to timed-plain lyrics, and
 * shows an honest "No lyrics" state when none are found.
 */
@Composable
fun LyricsScreen(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit = {},
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs) else 0f
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var loading by remember(track.id) { mutableStateOf(true) }
    var lines by remember(track.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var synced by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        loading = true
        // 1) Sidecar .lrc next to a local file wins (offline, user-provided).
        val sidecar = LrcParser.findSidecar(track.filePath)
        if (sidecar.isNotEmpty()) {
            lines = sidecar
            synced = true
        } else {
            // 2) Fall back to LRCLIB.
            val res = LrcLibProvider.fetch(track)
            lines = res.lines
            synced = res.synced
        }
        loading = false
    }

    val activeIndex by remember {
        derivedStateOf { LrcParser.activeIndex(lines, positionMs) }
    }

    val listState = rememberLazyListState()
    // Auto-scroll so the active line sits ~1/3 down the viewport.
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lines.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(activeIndex.coerceAtLeast(0), scrollOffset = -360) }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF0E2438),
                    0.62f to Color(0xFF091A2C),
                    1f to Color(0xFF040C18),
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF1E3D55), Color.Transparent),
                    center = Offset.Unspecified, radius = 1100f
                )
            )
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Top bar ──
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoverArt(track.artworkUri, track.colorKey, size = 42.dp, corner = 8.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            track.title,
                            style = BeatType.CardTitle.copy(fontSize = 15.sp),
                            color = BeatColors.TextPrimary
                        )
                        Text(
                            track.artist,
                            style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0x94FFFFFF),
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x6B000000))
                        .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClose
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            LyricsActions(
                hasLyrics = lines.isNotEmpty(),
                onCopy = { clipboard.setText(AnnotatedString(lines.joinToString("\n") { it.text })) },
                onShare = {
                    val text = lines.joinToString("\n") { it.text }.ifBlank { "${track.title} — ${track.artist}" }
                    runCatching {
                        context.startActivity(android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }, "Share lyrics"))
                    }
                },
                onReport = {
                    runCatching {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://lrclib.net")))
                    }
                }
            )

            // ── Lyrics body ──
            when {
                loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BeatColors.Accent)
                }
                lines.isEmpty() -> NoLyrics(Modifier.weight(1f))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 140.dp, bottom = 220.dp)
                ) {
                    itemsIndexed(lines) { i, line ->
                        LyricLineRow(
                            text = line.text,
                            state = when {
                                i < activeIndex -> LineState.Passed
                                i == activeIndex -> LineState.Active
                                else -> LineState.Upcoming
                            },
                            onClick = { if (synced) onSeek(line.timeMs) }
                        )
                    }
                }
            }
        }

        // ── Bottom transport pill (66dp) ──
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(Color(0xD110141C))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(33.dp))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // white play-mini (42dp)
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onTogglePlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    tint = Color.Black, modifier = Modifier.size(18.dp)
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x2EFFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentBrush)
                )
            }
            Text(
                positionMs.formatTime(),
                style = BeatType.TabLabel.copy(fontSize = 11.sp, letterSpacing = 0.em, fontWeight = FontWeight.SemiBold),
                color = Color(0x80FFFFFF)
            )
        }
    }
}

@Composable
private fun LyricsActions(
    hasLyrics: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LyricsPill("Copy", enabled = hasLyrics, onClick = onCopy)
        LyricsPill("Share", enabled = hasLyrics, onClick = onShare)
        LyricsPill("Report", enabled = true, onClick = onReport)
    }
}

@Composable
private fun LyricsPill(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Color(0x26000000) else Color(0x12000000))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = BeatType.Pill, color = if (enabled) Color.White else Color(0x66FFFFFF))
    }
}

private enum class LineState { Passed, Active, Upcoming }

@Composable
private fun LyricLineRow(text: String, state: LineState, onClick: () -> Unit) {
    val color = when (state) {
        LineState.Active -> Color.White
        LineState.Passed -> Color(0x38FFFFFF)
        LineState.Upcoming -> Color(0x66FFFFFF)
    }
    val size = if (state == LineState.Active) 34.sp else 28.sp
    Text(
        text,
        style = BeatType.LargeTitle.copy(
            fontSize = size,
            letterSpacing = (-0.026f).em,
            lineHeight = if (state == LineState.Active) 40.sp else 34.sp,
        ),
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 18.dp)
            .then(if (state == LineState.Active) Modifier.scale(1.02f) else Modifier)
    )
}

@Composable
private fun NoLyrics(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No synced lyrics", style = BeatType.SectionTitle, color = BeatColors.TextPrimary, textAlign = TextAlign.Center)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Text(
            "We couldn't find lyrics for this track on LRCLIB.",
            style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
        )
    }
}
