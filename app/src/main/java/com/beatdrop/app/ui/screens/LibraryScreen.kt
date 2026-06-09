package com.beatdrop.app.ui.screens

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.components.AlbumCarousel
import com.beatdrop.app.ui.components.CompactHeader
import com.beatdrop.app.ui.components.FilterPills
import com.beatdrop.app.ui.components.HeaderIcon
import com.beatdrop.app.ui.components.Hero
import com.beatdrop.app.ui.components.QuickGrid
import com.beatdrop.app.ui.components.ScreenBackground
import com.beatdrop.app.ui.components.ScreenTheme
import com.beatdrop.app.ui.components.SectionHeader
import com.beatdrop.app.ui.components.TrackRow
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/** Library is local/on-device only. Online/cloud lives in Home. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    currentTrackId: String?,
    isPlaying: Boolean,
    onOpenAlbum: (Album) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    displayName: String? = null,
    isSignedIn: Boolean = false,
    onSignIn: () -> Unit = {},
    vm: LibraryViewModel = viewModel(),
) {
    val permName = if (Build.VERSION.SDK_INT >= 33)
        android.Manifest.permission.READ_MEDIA_AUDIO
    else android.Manifest.permission.READ_EXTERNAL_STORAGE
    val perm = rememberPermissionState(permName)
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) vm.load()
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground(ScreenTheme.Library)
        when {
            !perm.status.isGranted -> LocalPermissionOnboarding { perm.launchPermissionRequest() }
            state.loading -> com.beatdrop.app.ui.components.SkeletonHomeContent()
            state.isEmpty -> EmptyLocalLibrary()
            else -> LocalLibraryContent(
                state = state,
                currentTrackId = currentTrackId,
                isPlaying = isPlaying,
                onOpenAlbum = onOpenAlbum,
                onOpenLiked = onOpenLiked,
                onOpenDownloads = onOpenDownloads,
                onPlayTracks = onPlayTracks,
                onSearch = onSearch,
                onAdd = onAdd,
                displayName = displayName,
            )
        }
    }
}

@Composable
private fun LocalPermissionOnboarding(onGrant: () -> Unit) {
    val t = rememberInfiniteTransition(label = "localPermission")
    val scale by t.animateFloat(0.95f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 34.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(86.dp)
                .scale(scale)
                .background(PillActiveBrush, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FolderOpen, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Unlock your local Library", style = BeatType.LargeTitle, color = BeatColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Give BeatDrop access to music saved on this phone. Local files stay on-device and keep playing even when you are offline.",
            style = BeatType.CardSub,
            color = BeatColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .background(PillActiveBrush, CircleShape)
                .clickable(onClick = onGrant)
                .padding(horizontal = 24.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Allow music access",
                style = BeatType.Pill,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

@Composable
private fun LocalLibraryContent(
    state: LibraryUiState,
    currentTrackId: String?,
    isPlaying: Boolean,
    onOpenAlbum: (Album) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    displayName: String?,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filters = remember { listOf("All", "Songs", "Albums", "Downloaded") }
    val albumLookup = remember(state.recent) { state.recent.associateBy { it.id } }
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 210.dp)
        ) {
            item {
                Hero(
                    greetingPlain = if (displayName.isNullOrBlank()) "On this phone" else "On this phone,",
                    greetingBold = displayName?.takeIf { it.isNotBlank() },
                    titlePlain = "Your",
                    titleAccent = "Library",
                    avatarText = displayName?.take(1),
                    onSearch = onSearch,
                    onAdd = onAdd,
                )
            }
            item { FilterPills(filters, filter) { filter = it } }
            item { Spacer(Modifier.height(18.dp)) }
            item {
                QuickGrid(state.quick) { q ->
                    if (q.albumId == "Liked Songs") onOpenLiked()
                    else if (q.albumId == "Downloads") onOpenDownloads()
                    else albumLookup[q.albumId]?.let(onOpenAlbum)
                }
            }
            item { SectionHeader("Songs", "Shuffle") }
            itemsIndexed(state.tracks, key = { _, t -> t.id }) { i, track ->
                TrackRow(
                    index = i + 1,
                    track = track,
                    isPlaying = track.id == currentTrackId && isPlaying,
                    onClick = { onPlayTracks(state.tracks, i) },
                )
            }
            if (state.recent.isNotEmpty()) {
                item { SectionHeader("Explore your catalogue", "") }
                item { AlbumCarousel(state.recent, onOpenAlbum) }
            }
        }
        CompactHeader(
            title = "Library",
            listState = listState,
            icons = listOf(
                HeaderIcon(Icons.Rounded.Search, "Search", onSearch),
                HeaderIcon(Icons.Rounded.LibraryMusic, "Add", onAdd),
            ),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun EmptyLocalLibrary() {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No local music found", style = BeatType.SectionTitle, color = BeatColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Add audio files to this phone and they’ll appear here.", style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center)
    }
}
