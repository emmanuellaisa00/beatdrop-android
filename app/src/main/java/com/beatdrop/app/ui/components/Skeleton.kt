package com.beatdrop.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmering placeholder primitives for skeleton-loading states.
 * A horizontal light sweep travels across muted glass blocks while real content
 * is being fetched, so each screen shows its layout shape instead of a bare spinner.
 */
@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -700f, targetValue = 700f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0x0FFFFFFF), Color(0x24FFFFFF), Color(0x0FFFFFFF)),
        start = Offset(x, 0f),
        end = Offset(x + 350f, 350f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp,
) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(shimmerBrush()))
}

/** Skeleton for a carousel row of album cards (matches AlbumCarousel layout). */
@Composable
fun SkeletonCarousel() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 3 cards are enough to fill the viewport; not scrollable while loading.
        repeat(3) {
            Column(Modifier.width(154.dp)) {
                SkeletonBox(Modifier.size(154.dp), corner = 12.dp)
                SkeletonBox(Modifier.padding(top = 12.dp).fillMaxWidth(0.9f).height(13.dp))
                SkeletonBox(Modifier.padding(top = 6.dp).fillMaxWidth(0.6f).height(11.dp))
            }
        }
    }
}

/** Skeleton for a vertical track list (matches TrackRow layout). */
@Composable
fun SkeletonTrackList(rows: Int = 8) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonBox(Modifier.size(44.dp), corner = 8.dp)
                Column(Modifier.weight(1f)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.7f).height(14.dp))
                    SkeletonBox(Modifier.padding(top = 6.dp).fillMaxWidth(0.45f).height(11.dp))
                }
                SkeletonBox(Modifier.width(30.dp).height(11.dp))
            }
        }
    }
}

@Composable
private fun SkeletonSectionTitle() {
    SkeletonBox(
        Modifier
            .padding(start = 20.dp, top = 30.dp, bottom = 14.dp)
            .width(160.dp)
            .height(22.dp),
        corner = 6.dp
    )
}

/** Home / Library skeleton: hero block, quick grid, two carousels, a few rows. */
@Composable
fun SkeletonHomeContent() {
    Column(Modifier.fillMaxWidth().padding(top = 70.dp)) {
        // hero
        Column(Modifier.padding(horizontal = 20.dp)) {
            SkeletonBox(Modifier.size(36.dp), corner = 18.dp)
            SkeletonBox(Modifier.padding(top = 18.dp).fillMaxWidth(0.55f).height(34.dp), corner = 8.dp)
        }
        // quick grid (2 columns)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBox(Modifier.weight(1f).height(58.dp), corner = 12.dp)
                    SkeletonBox(Modifier.weight(1f).height(58.dp), corner = 12.dp)
                }
            }
        }
        SkeletonSectionTitle()
        SkeletonCarousel()
        SkeletonSectionTitle()
        SkeletonCarousel()
    }
}

/** Detail (album/artist/playlist) skeleton: big cover + meta + track list. */
@Composable
fun SkeletonDetailContent() {
    Column(
        Modifier.fillMaxWidth().padding(top = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SkeletonBox(Modifier.size(210.dp), corner = 16.dp)
        SkeletonBox(Modifier.padding(top = 22.dp).width(180.dp).height(24.dp), corner = 6.dp)
        SkeletonBox(Modifier.padding(top = 10.dp).width(120.dp).height(13.dp))
        Spacer(Modifier.height(28.dp))
        SkeletonTrackList(7)
    }
}
