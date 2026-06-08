package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.local.PlaylistStore
import com.beatdrop.app.data.local.UserPlaylist
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush

/**
 * "Add to playlist" sheet — pick an existing user playlist or create a new one,
 * then persist the track into it (PlaylistStore). Works for local & online tracks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(track: Track, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    var playlists by remember { mutableStateOf(store.all()) }
    var showCreate by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF120F16),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Add to playlist",
                style = BeatType.SectionTitle,
                color = BeatColors.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
            )

            // New playlist row
            Row(
                Modifier.fillMaxWidth().clickable { showCreate = true }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(PillActiveBrush),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, "New", tint = Color.White, modifier = Modifier.size(22.dp)) }
                Text("New playlist", style = BeatType.TrackTitle, color = BeatColors.TextPrimary)
            }

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(playlists) { pl ->
                    val contains = track.id in pl.trackIds
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(enabled = !contains) {
                                store.addTracks(pl.id, listOf(track.id))
                                playlists = store.all()
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0x14FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.QueueMusic, null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(20.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, style = BeatType.TrackTitle, color = BeatColors.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${pl.trackIds.size} songs", style = BeatType.TrackSub, color = Color(0x7AFFFFFF))
                        }
                        if (contains) Icon(Icons.Rounded.Check, "Added", tint = BeatColors.Accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = Color(0xFF15121A),
            title = { Text("New playlist", color = BeatColors.TextPrimary, style = BeatType.SectionTitle) },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text("Playlist name", color = BeatColors.TextMuted) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val pl = store.create(name.ifBlank { "New Playlist" })
                    store.addTracks(pl.id, listOf(track.id))
                    showCreate = false
                    onDismiss()
                }) { Text("Create & add", color = BeatColors.Accent) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel", color = BeatColors.TextSecondary) } }
        )
    }
}
