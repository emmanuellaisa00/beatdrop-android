package com.beatdrop.app.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.ui.components.AlbumCarousel
import com.beatdrop.app.ui.components.CompactHeader
import com.beatdrop.app.ui.components.FilterPills
import com.beatdrop.app.ui.components.HeaderIcon
import com.beatdrop.app.ui.components.Hero
import com.beatdrop.app.ui.components.QuickGrid
import com.beatdrop.app.ui.components.ScreenBackground
import com.beatdrop.app.ui.components.ScreenTheme
import com.beatdrop.app.ui.components.SectionHeader
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val permName = if (Build.VERSION.SDK_INT >= 33)
        android.Manifest.permission.READ_MEDIA_AUDIO
    else android.Manifest.permission.READ_EXTERNAL_STORAGE

    val perm = rememberPermissionState(permName) { granted -> vm.onPermissionResult(granted) }
    val state by vm.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) vm.onPermissionResult(true)
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground(ScreenTheme.Home)

        when {
            !perm.status.isGranted -> PermissionPrompt { perm.launchPermissionRequest() }
            state.loading -> com.beatdrop.app.ui.components.SkeletonHomeContent()
            state.isEmpty -> EmptyLibrary()
            else -> HomeContent(state, onOpenAlbum, onOpenLiked, onOpenDownloads, onSearch, onAdd)
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpenAlbum: (Album) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filters = remember { listOf("All", "Music", "Podcasts") }
    val albumLookup = remember(state.shelves) {
        state.shelves.flatMap { it.items }.associateBy { it.id }
    }
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 180.dp)
        ) {
            item {
                Hero(
                    greetingPlain = "Good evening,",
                    greetingBold = "Alex",
                    titlePlain = "Good",
                    titleAccent = "vibes",
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
            state.shelves.forEach { shelf ->
                item { SectionHeader(shelf.title, shelf.seeAllLabel) }
                item { AlbumCarousel(shelf.items, onOpenAlbum) }
            }
        }

        CompactHeader(
            title = "Home",
            listState = listState,
            icons = listOf(
                HeaderIcon(Icons.Rounded.Search, "Search", onSearch),
                HeaderIcon(Icons.Rounded.Add, "Add", onAdd),
            ),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Let BeatDrop play your music", style = BeatType.SectionTitle, color = BeatColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Grant access to your audio library to start listening. An online catalogue will be added later.",
            style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        androidx.compose.material3.Button(onClick = onGrant) { Text("Grant access") }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No music found", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add some audio files to your device, or connect the online catalogue (coming soon).",
            style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
        )
    }
}
