package com.beatdrop.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.DownloadManager
import com.beatdrop.app.ui.screens.formatTime
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/**
 * Track row — ported from `.track` / `.track.playing` in the HTML.
 * num 22dp, art 44dp r8, title 15/600, sub 12.5/500, dur 13/500@.38, more 18dp.
 * When playing: faint accent bg, accent title (700), and the animated 3-bar equalizer
 * replacing the index number.
 */
@Composable
fun TrackRow(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
) {
    val trackMenu = LocalTrackMenu.current
    val moreAction = onMore ?: { trackMenu(track) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPlaying) Color(0x12FF375F) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // num / equalizer
        Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
            if (isPlaying) Equalizer()
            else Text(
                "$index",
                style = BeatType.TrackTitle.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = BeatColors.TextMuted
            )
        }
        CoverArt(track.artworkUri, track.colorKey, size = 44.dp, corner = 8.dp, glyphSize = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = BeatType.TrackTitle.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isPlaying) BeatColors.Accent else BeatColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = BeatType.TrackSub,
                color = Color(0x7AFFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        // Online tracks show a download button instead of a duration label.
        if (track.source == MediaSource.ONLINE) {
            val jobs by DownloadManager.jobs.collectAsState()
            val downloaded by DownloadManager.downloaded.collectAsState()
            val download = LocalDownload.current
            val cancel = LocalCancelDownload.current
            DownloadButton(
                job = jobs[track.id],
                isDownloaded = downloaded.any { it.id == track.id },
                onDownload = { download(track) },
                onCancel = { cancel(track.id) },
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    "On device",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    track.durationMs.formatTime(),
                    style = BeatType.TrackSub.copy(fontSize = 13.sp),
                    color = Color(0x61FFFFFF)
                )
            }
        }
        Icon(
            Icons.Rounded.MoreVert, "More",
            tint = Color(0x80FFFFFF),
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = moreAction)
        )
    }
}

/** 3-bar accent equalizer — heights 40/90/60%, 750ms, staggered (matches @keyframes eq). */
@Composable
private fun Equalizer() {
    val t = rememberInfiniteTransition(label = "eq")
    val heights = listOf(0.40f, 0.90f, 0.60f)
    val delays = listOf(-300, -100, -500)
    Row(
        Modifier.height(14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        heights.forEachIndexed { i, peak ->
            val scale by t.animateFloat(
                initialValue = 0.25f,
                targetValue = peak,
                animationSpec = infiniteRepeatable(
                    tween(750, delayMillis = 0),
                    RepeatMode.Reverse,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(
                        (750 + delays[i]).coerceAtLeast(0)
                    )
                ),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BeatColors.Accent)
            )
        }
    }
}
