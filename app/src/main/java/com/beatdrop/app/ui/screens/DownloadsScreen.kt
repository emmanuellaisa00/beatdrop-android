package com.beatdrop.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.components.CoverArt
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush

/**
 * Dedicated Downloads view — offline songs saved on the device, with storage
 * usage, play / shuffle, and per-row delete.
 */
@Composable
fun DownloadsScreen(
    tracks: List<Track>,
    usageLabel: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onDelete: (Track) -> Unit,
) {
    val listState = rememberLazyListState()
    BackHandler(onBack = onBack)
    val thresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    val frosted by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > thresholdPx
        }
    }

    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp)
        ) {
            item { DownloadsHero(count = tracks.size, usageLabel = usageLabel) }
            if (tracks.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircleBtn(Icons.Rounded.Shuffle, "Shuffle") { onPlayTracks(tracks.shuffled(), 0) }
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(58.dp).clip(CircleShape).background(PillActiveBrush)
                                .clickable { onPlayTracks(tracks, 0) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp)) }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                itemsIndexed(tracks, key = { _, t -> t.id }) { i, track ->
                    DownloadedRow(
                        index = i + 1,
                        track = track,
                        isPlaying = track.id == currentTrackId && isPlaying,
                        onClick = { onPlayTracks(tracks, i) },
                        onDelete = { onDelete(track) },
                    )
                }
            } else {
                item { EmptyDownloads() }
            }
        }
        StickyBackBar(
            title = "Downloads",
            frosted = frosted,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun DownloadsHero(count: Int, usageLabel: String) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(320.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF0F9855), BeatColors.Background)))
        )
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 110.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(170.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF25D278), Color(0xFF0F9855)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.DownloadDone, null, tint = Color.White, modifier = Modifier.size(64.dp)) }
            Text(
                "Downloads",
                style = BeatType.LargeTitle.copy(fontSize = 30.sp, letterSpacing = (-0.034f).em),
                color = BeatColors.TextPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                if (count == 0) "Songs you download play offline" else "$count songs · $usageLabel",
                style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color(0xA6FFFFFF),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun DownloadedRow(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPlaying) Color(0x12FF375F) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoverArt(track.artworkUri, track.colorKey, size = 44.dp, corner = 8.dp, glyphSize = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = BeatType.TrackTitle.copy(fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold),
                color = if (isPlaying) BeatColors.Accent else BeatColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.DownloadDone, "Offline", tint = BeatColors.Accent, modifier = Modifier.size(13.dp))
                Text(
                    track.artist, style = BeatType.TrackSub, color = Color(0x7AFFFFFF),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            Icons.Rounded.Delete, "Delete download",
            tint = Color(0x99FFFFFF),
            modifier = Modifier.size(20.dp).clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun EmptyDownloads() {
    Column(
        Modifier.fillMaxWidth().padding(top = 30.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No downloads yet", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the download icon on any online song to save it for offline playback.",
            style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color(0x14FFFFFF)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = Color(0xE0FFFFFF), modifier = Modifier.size(20.dp)) }
}
