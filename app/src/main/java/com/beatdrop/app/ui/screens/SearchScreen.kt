package com.beatdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.components.AlbumCarousel
import com.beatdrop.app.ui.components.BrowseGrid
import com.beatdrop.app.ui.components.BrowseTile
import com.beatdrop.app.ui.components.CompactHeader
import com.beatdrop.app.ui.components.HeaderIcon
import com.beatdrop.app.ui.components.Hero
import com.beatdrop.app.ui.components.ScreenBackground
import com.beatdrop.app.ui.components.ScreenTheme
import com.beatdrop.app.ui.components.SectionHeader
import com.beatdrop.app.ui.components.TrackRow
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.BrowsePalette

@Composable
fun SearchScreen(
    currentTrackId: String?,
    isPlaying: Boolean,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onAdd: () -> Unit,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val genres = remember {
        listOf(
            BrowseTile("Pop", BrowsePalette.BT1, Icons.Rounded.GraphicEq, "Pop Mix"),
            BrowseTile("Hip-Hop", BrowsePalette.BT4, Icons.Rounded.MusicNote, "Hip-Hop Mix"),
            BrowseTile("Chill", BrowsePalette.BT3, Icons.Rounded.NightlightRound, "Chill Mix"),
            BrowseTile("Indie", BrowsePalette.BT5, Icons.Rounded.Star, "Indie Mix"),
            BrowseTile("Electronic", BrowsePalette.BT2, Icons.Rounded.Bolt, "Electronic Mix"),
            BrowseTile("R&B", BrowsePalette.BT6, Icons.Rounded.Favorite, "R&B Mix"),
            BrowseTile("Jazz", BrowsePalette.BT7, Icons.Rounded.MusicNote, "Jazz Mix"),
            BrowseTile("Workout", BrowsePalette.BT8, Icons.Rounded.Equalizer, "Workout Mix"),
        )
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground(ScreenTheme.Search)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 200.dp)
        ) {
            item {
                Hero(
                    greetingPlain = "Search",
                    titlePlain = "Find your",
                    titleAccent = "sound",
                    showActions = false,
                )
            }
            item {
                SearchField(
                    query = state.query,
                    onQueryChange = vm::onQueryChange,
                    onClear = vm::clear,
                )
            }
            item { CategoryToggle(category = state.category, onSelect = vm::setCategory) }

            if (state.hasQuery) {
                if (state.loading) {
                    item { SectionHeader(categoryTitle(state.category), "") }
                    when (state.category) {
                        SearchCategory.SONGS, SearchCategory.ARTISTS -> item { com.beatdrop.app.ui.components.SkeletonTrackList(6) }
                        SearchCategory.ALBUMS, SearchCategory.PLAYLISTS -> item { com.beatdrop.app.ui.components.SkeletonCarousel() }
                    }
                } else {
                    when (state.category) {
                        SearchCategory.SONGS -> {
                            if (state.resultsTracks.isNotEmpty()) {
                                item { SectionHeader("Songs", "Online") }
                                itemsIndexed(state.resultsTracks, key = { _, t -> t.id }) { i, track ->
                                    TrackRow(
                                        index = i + 1,
                                        track = track,
                                        isPlaying = track.id == currentTrackId && isPlaying,
                                        onClick = { onPlayTracks(state.resultsTracks, i) },
                                    )
                                }
                            } else item { NoResults(state.query) }
                        }
                        SearchCategory.ALBUMS -> {
                            if (state.resultsAlbums.isNotEmpty()) {
                                item { SectionHeader("Albums", "Online") }
                                item { AlbumCarousel(state.resultsAlbums, onOpenAlbum) }
                            } else item { NoResults(state.query) }
                        }
                        SearchCategory.PLAYLISTS -> {
                            if (state.resultsPlaylists.isNotEmpty()) {
                                item { SectionHeader("Playlists", "Online") }
                                item { AlbumCarousel(state.resultsPlaylists, onOpenAlbum) }
                            } else item { NoResults(state.query) }
                        }
                        SearchCategory.ARTISTS -> {
                            if (state.resultsArtists.isNotEmpty()) {
                                item { SectionHeader("Artists", "Online") }
                                items(state.resultsArtists, key = { it }) { artist ->
                                    ArtistResultRow(artist = artist, onClick = { onOpenArtist(artist) })
                                }
                            } else item { NoResults(state.query) }
                        }
                    }
                }
            } else {
                if (state.recentSearches.isNotEmpty()) {
                    item { SectionHeader("Recent searches", "Clear", onAction = vm::clearRecent) }
                    items(state.recentSearches, key = { it }) { query ->
                        RecentSearchRow(query = query, onClick = { vm.onQueryChange(query) })
                    }
                }
                item { SectionHeader("Online genres", "") }
                item { BrowseGrid(genres) { vm.onQueryChange(it.label) } }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        CompactHeader(
            title = "Search",
            listState = listState,
            icons = listOf(HeaderIcon(Icons.Rounded.Search, "Search") { }),
            thresholdDp = 40,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(25.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Search, "Search", tint = Color(0x8CFFFFFF), modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search online songs, albums, playlists, artists",
                    style = BeatType.CardSub.copy(fontSize = 14.sp),
                    color = Color(0x73FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = BeatType.CardSub.copy(fontSize = 14.sp, color = Color.White),
                cursorBrush = SolidColor(BeatColors.Accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close, "Clear",
                tint = Color(0x99FFFFFF),
                modifier = Modifier.size(18.dp).clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun CategoryToggle(category: SearchCategory, onSelect: (SearchCategory) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x14FFFFFF))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SearchCategory.entries.forEach { c ->
            val active = c == category
            Box(
                Modifier
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (active) BeatColors.Accent else Color.Transparent)
                    .clickable { onSelect(c) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    categoryLabel(c),
                    style = BeatType.Pill,
                    color = if (active) Color.White else BeatColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(query: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(18.dp))
        }
        Text(
            query,
            style = BeatType.TrackTitle.copy(fontWeight = FontWeight.SemiBold),
            color = BeatColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ArtistResultRow(artist: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x17FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                artist,
                style = BeatType.TrackTitle.copy(fontWeight = FontWeight.SemiBold),
                color = BeatColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Artist", style = BeatType.TrackSub, color = Color(0x7AFFFFFF))
        }
    }
}

private fun categoryLabel(category: SearchCategory): String = when (category) {
    SearchCategory.SONGS -> "Songs"
    SearchCategory.ALBUMS -> "Albums"
    SearchCategory.PLAYLISTS -> "Playlists"
    SearchCategory.ARTISTS -> "Artists"
}

private fun categoryTitle(category: SearchCategory): String = categoryLabel(category)

@Composable
private fun NoResults(query: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No online results for \"$query\"", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Try a different artist, song, album, or playlist name.",
            style = BeatType.CardSub,
            color = BeatColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
