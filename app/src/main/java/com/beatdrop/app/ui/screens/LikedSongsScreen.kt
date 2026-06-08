package com.beatdrop.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.components.TrackRow
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush

/** Liked Songs — the special playlist behind the heart, fed by LikesViewModel. */
@Composable
fun LikedSongsScreen(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
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
            item { LikedHero(count = tracks.size) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircleBtn(Icons.Rounded.Shuffle, "Shuffle", enabled = tracks.isNotEmpty()) {
                        if (tracks.isNotEmpty()) onPlayTracks(tracks.shuffled(), 0)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(PillActiveBrush)
                            .clickable(enabled = tracks.isNotEmpty()) { if (tracks.isNotEmpty()) onPlayTracks(tracks, 0) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }

            if (tracks.isEmpty()) {
                item { EmptyLiked() }
            } else {
                itemsIndexed(tracks, key = { _, t -> t.id }) { i, track ->
                    TrackRow(
                        index = i + 1,
                        track = track,
                        isPlaying = track.id == currentTrackId && isPlaying,
                        onClick = { onPlayTracks(tracks, i) },
                    )
                }
            }
        }
        StickyBackBar(
            title = "Liked Songs",
            frosted = frosted,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LikedHero(count: Int) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(330.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF5A2238), BeatColors.Background)))
        )
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 110.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Liked-songs cover: accent gradient with a big heart
            Box(
                Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(PillActiveBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(70.dp))
            }
            Text(
                "Liked Songs",
                style = BeatType.LargeTitle.copy(fontSize = 30.sp, letterSpacing = (-0.034f).em),
                color = BeatColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                if (count == 0) "Songs you like will appear here" else "$count songs",
                style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color(0xA6FFFFFF),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun EmptyLiked() {
    Column(
        Modifier.fillMaxWidth().padding(top = 30.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No liked songs yet", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the heart on any song to save it here.",
            style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color(0x14FFFFFF))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = Color(0xE0FFFFFF), modifier = Modifier.size(20.dp)) }
}
