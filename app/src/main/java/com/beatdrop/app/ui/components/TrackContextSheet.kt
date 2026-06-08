package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/** Actions a track context sheet can emit. */
data class TrackActions(
    val onToggleLike: (Track) -> Unit,
    val onPlayNext: (Track) -> Unit,
    val onAddToQueue: (Track) -> Unit,
    val onAddToPlaylist: (Track) -> Unit,
    val onGoToArtist: (Track) -> Unit,
    val onGoToAlbum: (Track) -> Unit,
    val onShare: (Track) -> Unit,
)

/**
 * Global track ⋯ menu — Spotify-style context sheet opened from any track's
 * "more" button. Header shows the track; rows are the available actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextSheet(
    track: Track,
    isLiked: Boolean,
    actions: TrackActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF120F16),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverArt(track.artworkUri, track.colorKey, size = 48.dp, corner = 8.dp, glyphSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title, style = BeatType.CardTitle.copy(fontSize = 16.sp),
                        color = BeatColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist, style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0x94FFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 20.dp)) {
                Box(Modifier.fillMaxWidth().size(1.dp).background(Color(0x14FFFFFF)))
            }

            ActionRow(
                if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                tint = if (isLiked) BeatColors.Accent else null,
            ) { actions.onToggleLike(track); onDismiss() }
            ActionRow(Icons.Rounded.PlayArrow, "Play next") { actions.onPlayNext(track); onDismiss() }
            ActionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue") { actions.onAddToQueue(track); onDismiss() }
            ActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist") { actions.onAddToPlaylist(track); onDismiss() }
            ActionRow(Icons.Rounded.Person, "Go to artist") { actions.onGoToArtist(track); onDismiss() }
            ActionRow(Icons.Rounded.Album, "Go to album") { actions.onGoToAlbum(track); onDismiss() }
            ActionRow(Icons.Rounded.IosShare, "Share") { actions.onShare(track); onDismiss() }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(icon, label, tint = tint ?: Color(0xD9FFFFFF), modifier = Modifier.size(22.dp))
        Text(label, style = BeatType.TrackTitle, color = tint ?: BeatColors.TextPrimary)
    }
}
