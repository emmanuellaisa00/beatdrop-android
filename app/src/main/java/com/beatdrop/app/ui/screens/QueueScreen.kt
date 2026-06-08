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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.player.PlaybackState
import com.beatdrop.app.player.RepeatMode
import com.beatdrop.app.ui.components.CoverArt
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/**
 * Queue — the hidden "Now playing / Next up" screen, backed by the REAL ExoPlayer queue.
 * Tap a row to jump, reorder with up/down handles, remove with ✕, toggle shuffle/repeat.
 * Styled to match the Now Playing surface (dark teal gradient + glass rows).
 */
@Composable
fun QueueScreen(
    pb: PlaybackState,
    onClose: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val current = pb.queue.getOrNull(pb.currentIndex)
    val upNext = if (pb.queue.isNotEmpty()) pb.queue.drop(pb.currentIndex + 1) else emptyList()
    val upNextStart = pb.currentIndex + 1

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
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar: close | "Queue" | shuffle+repeat
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleBtn(Icons.Rounded.KeyboardArrowDown, "Close", onClick = onClose)
                Text("Queue", style = BeatType.TopBarTitle, color = BeatColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleBtn(Icons.Rounded.Shuffle, "Shuffle", active = pb.shuffle, onClick = onToggleShuffle)
                    ToggleBtn(
                        if (pb.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        active = pb.repeat != RepeatMode.OFF,
                        onClick = onCycleRepeat
                    )
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                // Now playing
                if (current != null) {
                    item {
                        SectionLabel("Now playing")
                        QueueRow(
                            track = current,
                            isCurrent = true,
                            canMoveUp = false,
                            canMoveDown = false,
                            onClick = {},
                            onUp = {}, onDown = {}, onRemove = {},
                        )
                    }
                }
                // Up next
                if (upNext.isNotEmpty()) {
                    item { SectionLabel("Next up") }
                    itemsIndexed(upNext, key = { _, t -> t.id + "_q" }) { i, track ->
                        val absoluteIndex = upNextStart + i
                        QueueRow(
                            track = track,
                            isCurrent = false,
                            canMoveUp = i > 0,
                            canMoveDown = i < upNext.lastIndex,
                            onClick = { onJumpTo(absoluteIndex) },
                            onUp = { onMove(absoluteIndex, absoluteIndex - 1) },
                            onDown = { onMove(absoluteIndex, absoluteIndex + 1) },
                            onRemove = { onRemove(absoluteIndex) },
                        )
                    }
                } else if (current != null) {
                    item {
                        Text(
                            "End of queue",
                            style = BeatType.CardSub,
                            color = Color(0x73FFFFFF),
                            modifier = Modifier.padding(start = 24.dp, top = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = BeatType.SeeAll.copy(fontSize = 12.sp, letterSpacing = 0.06.em),
        color = Color(0x99FFFFFF),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) Color(0x12FF375F) else Color.Transparent)
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoverArt(track.artworkUri, track.colorKey, size = 44.dp, corner = 8.dp, glyphSize = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = BeatType.TrackTitle.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold),
                color = if (isCurrent) BeatColors.Accent else BeatColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = BeatType.TrackSub,
                color = Color(0x7AFFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            // small accent equalizer-like dot to mark the current item
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(BeatColors.Accent)
            )
        } else {
            // reorder + remove handles
            SmallIcon(Icons.Rounded.KeyboardArrowUp, "Move up", enabled = canMoveUp, onClick = onUp)
            SmallIcon(Icons.Rounded.KeyboardArrowDown, "Move down", enabled = canMoveDown, onClick = onDown)
            SmallIcon(Icons.Rounded.Close, "Remove", enabled = true, onClick = onRemove)
        }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Icon(
        icon, desc,
        tint = if (enabled) Color(0xB3FFFFFF) else Color(0x33FFFFFF),
        modifier = Modifier
            .size(22.dp)
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun CircleBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x52000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ToggleBtn(icon: ImageVector, desc: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) Color(0x33FF375F) else Color(0x52000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, desc,
            tint = if (active) BeatColors.Accent else Color(0xCCFFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}
