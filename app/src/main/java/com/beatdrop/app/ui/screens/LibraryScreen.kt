package com.beatdrop.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
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

/**
 * Library — ported from #screen-library. Reproduces the HTML scroll behavior:
 * the hero (avatar/greeting + "Your Library") scrolls away, and a frosted
 * compact header ("Library" + search/add icons) pins at the top once scrolled
 * past ~120px.
 */
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
    vm: LibraryViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground(ScreenTheme.Library)

        when {
            state.loading -> com.beatdrop.app.ui.components.SkeletonHomeContent()
            state.isEmpty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No music found", color = BeatColors.TextSecondary)
            }
            else -> LibraryContent(state, currentTrackId, isPlaying, onOpenAlbum, onOpenLiked, onOpenDownloads, onPlayTracks, onSearch, onAdd)
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    currentTrackId: String?,
    isPlaying: Boolean,
    onOpenAlbum: (Album) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filters = remember { listOf("Playlists", "Albums", "Artists", "Downloaded", "Recently played") }
    val listState = rememberLazyListState()
    val albumLookup = remember(state.recent) { state.recent.associateBy { it.id } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 200.dp)
        ) {
            item {
                Hero(
                    greetingPlain = "Good evening,",
                    greetingBold = "Alex",
                    titlePlain = "Your",
                    titleAccent = "Library",
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

            item { SectionHeader("Recently played", "See all") }
            item { AlbumCarousel(state.recent, onOpenAlbum) }

            item { SectionHeader("From your library", "Shuffle") }
            itemsIndexed(state.tracks, key = { _, t -> t.id }) { i, track ->
                TrackRow(
                    index = i + 1,
                    track = track,
                    isPlaying = track.id == currentTrackId && isPlaying,
                    onClick = { onPlayTracks(state.tracks, i) },
                )
            }
        }

        // Pinned frosted header — appears once scrolled past the large title.
        CompactHeader(
            title = "Library",
            listState = listState,
            icons = listOf(
                HeaderIcon(Icons.Rounded.Search, "Search", onSearch),
                HeaderIcon(Icons.Rounded.Add, "Add", onAdd),
            ),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
