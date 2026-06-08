package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.QuickItem
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush

@Composable
fun SectionHeader(title: String, action: String, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
        Text(
            action.uppercase(),
            style = BeatType.SeeAll,
            color = BeatColors.TextMuted,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@Composable
fun FilterPills(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
    ) {
        items(items.size) { i ->
            val active = i == selected
            Box(
                Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .then(
                        if (active) Modifier.background(PillActiveBrush)
                        else Modifier
                            .background(BeatColors.Surface)
                            .border(1.dp, BeatColors.Surface, RoundedCornerShape(17.dp))
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    items[i],
                    style = BeatType.Pill,
                    color = if (active) BeatColors.TextPrimary else BeatColors.TextSecondary
                )
            }
        }
    }
}

/** 2-column quick-access grid (Liked Songs, Downloads, recent albums...). */
@Composable
fun QuickGrid(items: List<QuickItem>, onClick: (QuickItem) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    QuickTile(item, Modifier.weight(1f)) { onClick(item) }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTile(item: QuickItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BeatColors.Surface)
            .border(1.dp, BeatColors.Surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            CoverArt(artworkUri = null, colorKey = item.colorKey, size = 58.dp, corner = 0.dp)
            val icon = when (item.title) {
                "Liked Songs" -> Icons.Rounded.Favorite
                "Downloads" -> Icons.Rounded.DownloadDone
                else -> Icons.Rounded.MusicNote
            }
            Icon(
                icon,
                contentDescription = null,
                tint = if (item.title == "Liked Songs") Color(0xFFFF375F) else if (item.title == "Downloads") Color(0xFF1DB954) else Color(0x99FFFFFF),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            item.title,
            style = BeatType.QuickName,
            color = BeatColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

/** Horizontal carousel of album cards (154dp covers). */
@Composable
fun AlbumCarousel(albums: List<Album>, onClick: (Album) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        items(albums) { album ->
            Column(Modifier.width(154.dp).clickable { onClick(album) }) {
                CoverArt(album.artworkUri, album.colorKey, size = 154.dp, corner = 12.dp)
                Text(
                    album.title,
                    style = BeatType.CardTitle,
                    color = BeatColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    album.artist,
                    style = BeatType.CardSub,
                    color = BeatColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
