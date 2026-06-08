package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.repository.MusicRepository
import com.beatdrop.app.ui.components.CoverArt
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.components.TrackRow
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.CoverPalette
import com.beatdrop.app.ui.theme.PillActiveBrush
import com.beatdrop.app.ui.theme.brush
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks

    private var currentId: String = ""

    fun load(playlistId: String) {
        currentId = playlistId
        viewModelScope.launch {
            _album.value = repo.playlistAsAlbum(playlistId)
            if (_allTracks.value.isEmpty()) _allTracks.value = repo.allTracks()
        }
    }

    fun addTracks(trackIds: List<String>) {
        viewModelScope.launch {
            repo.addTracksToPlaylist(currentId, trackIds)
            repo.pushPlaylistSnapshot(currentId)
            _album.value = repo.playlistAsAlbum(currentId)
        }
    }
}

/**
 * Playlist detail — same visual recipe as the album hero, for a user-created
 * playlist. Adds an empty state + "Add tracks" flow (real device tracks) via a
 * modal bottom sheet, persisting selections to the on-device PlaylistStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    vm: PlaylistDetailViewModel = viewModel(),
) {
    LaunchedEffect(playlistId) { vm.load(playlistId) }
    val album by vm.album.collectAsStateWithLifecycle()
    val allTracks by vm.allTracks.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    BackHandler(onBack = onBack)

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val thresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    val frosted by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > thresholdPx
        }
    }

    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        val a = album
        if (a == null) {
            com.beatdrop.app.ui.components.SkeletonDetailContent()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 200.dp)
            ) {
                item { PlaylistHero(a) }
                item {
                    PlaylistActions(
                        hasTracks = a.tracks.isNotEmpty(),
                        onAdd = { showSheet = true },
                        onShuffle = { if (a.tracks.isNotEmpty()) onPlayTracks(a.tracks.shuffled(), 0) },
                        onPlay = { if (a.tracks.isNotEmpty()) onPlayTracks(a.tracks, 0) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }

                if (a.tracks.isEmpty()) {
                    item { EmptyPlaylist(onAdd = { showSheet = true }) }
                } else {
                    itemsIndexed(a.tracks, key = { _, t -> t.id }) { i, track ->
                        TrackRow(
                            index = i + 1,
                            track = track,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            onClick = { onPlayTracks(a.tracks, i) },
                        )
                    }
                }
            }
        }

        StickyBackBar(
            title = album?.title ?: "",
            frosted = frosted,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showSheet) {
        val existing = album?.tracks?.map { it.id }?.toSet() ?: emptySet()
        AddTracksSheet(
            sheetState = sheetState,
            allTracks = allTracks,
            alreadyAdded = existing,
            onDismiss = { showSheet = false },
            onConfirm = { ids ->
                vm.addTracks(ids)
                showSheet = false
            }
        )
    }
}

@Composable
private fun PlaylistHero(album: Album) {
    val palette = CoverPalette.from(album.colorKey)
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .scale(1.25f)
                .alpha(0.75f)
                .blur(70.dp)
                .background(palette.brush())
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to BeatColors.Background,
                    )
                )
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 110.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoverArt(album.artworkUri, album.colorKey, size = 210.dp, corner = 16.dp, glyphSize = 76.dp)
            Text(
                album.title,
                style = BeatType.LargeTitle.copy(fontSize = 25.sp, letterSpacing = (-0.030f).em),
                color = BeatColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 22.dp)
            )
            Text(
                "${album.artist} · Playlist",
                style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color(0xA6FFFFFF),
                modifier = Modifier.padding(top = 6.dp)
            )
            val mins = (album.tracks.sumOf { it.durationMs } / 60000).toInt()
            Text(
                if (album.tracks.isEmpty()) "No songs yet" else "${album.tracks.size} songs · $mins min",
                style = BeatType.CardSub.copy(fontSize = 12.sp),
                color = Color(0x6BFFFFFF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PlaylistActions(hasTracks: Boolean, onAdd: () -> Unit, onShuffle: () -> Unit, onPlay: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircleAction(Icons.Rounded.Add, "Add tracks", onClick = onAdd)
        if (hasTracks) CircleAction(Icons.Rounded.Shuffle, "Shuffle", onClick = onShuffle)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(PillActiveBrush)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onPlay
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun CircleAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x14FFFFFF), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color(0xE0FFFFFF), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyPlaylist(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Let's build this playlist", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add songs from your library to get started.",
            style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(PillActiveBrush)
                .clickable(onClick = onAdd)
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("Add tracks", style = BeatType.Pill, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTracksSheet(
    sheetState: androidx.compose.material3.SheetState,
    allTracks: List<Track>,
    alreadyAdded: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val selected = remember { mutableStateListOf<String>() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF120F16),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Add to playlist", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected.isEmpty()) Color(0x14FFFFFF) else PillActiveBrush)
                        .clickable(enabled = selected.isNotEmpty()) { onConfirm(selected.toList()) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        if (selected.isEmpty()) "Done" else "Add ${selected.size}",
                        style = BeatType.Pill,
                        color = if (selected.isEmpty()) BeatColors.TextSecondary else Color.White
                    )
                }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                itemsIndexed(allTracks, key = { _, t -> t.id }) { _, track ->
                    val isAdded = track.id in alreadyAdded
                    val isSel = track.id in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAdded) {
                                if (isSel) selected.remove(track.id) else selected.add(track.id)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoverArt(track.artworkUri, track.colorKey, size = 44.dp, corner = 8.dp, glyphSize = 18.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                track.title,
                                style = BeatType.TrackTitle,
                                color = if (isAdded) BeatColors.TextMuted else BeatColors.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.artist,
                                style = BeatType.TrackSub,
                                color = Color(0x7AFFFFFF),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        // checkbox / state
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSel || isAdded) BeatColors.Accent else Color(0x14FFFFFF))
                                .border(1.dp, Color(0x29FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel || isAdded) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
