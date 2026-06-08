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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Artist
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.repository.MusicRepository
import com.beatdrop.app.ui.components.AlbumCarousel
import com.beatdrop.app.ui.components.SectionHeader
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

class ArtistDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist
    fun load(name: String) {
        viewModelScope.launch { _artist.value = repo.artistByName(name) }
    }
}

/**
 * Artist detail — Spotify-iOS architecture, same visual recipe as the album hero.
 * Tall artist image header that fades into the page, big name, listeners line,
 * Play/Shuffle, Popular tracks, then an Albums carousel. Frosted sticky-back on scroll.
 * (No artist screen exists in the HTML — designed to match its language.)
 */
@Composable
fun ArtistDetailScreen(
    artistName: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    vm: ArtistDetailViewModel = viewModel(),
) {
    LaunchedEffect(artistName) { vm.load(artistName) }
    val artist by vm.artist.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    BackHandler(onBack = onBack)

    val thresholdPx = with(LocalDensity.current) { 220.dp.toPx() }
    val frosted by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > thresholdPx
        }
    }

    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        val ar = artist
        if (ar == null) {
            com.beatdrop.app.ui.components.SkeletonDetailContent()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 200.dp)
            ) {
                item { ArtistHeader(ar, onPlay = { onPlayTracks(ar.allTracks(), 0) }, onShuffle = {
                    val all = ar.allTracks()
                    if (all.isNotEmpty()) onPlayTracks(all.shuffled(), 0)
                }) }

                if (ar.popularTracks.isNotEmpty()) {
                    item { SectionHeader("Popular", "") }
                    itemsIndexed(ar.popularTracks, key = { _, t -> t.id }) { i, track ->
                        TrackRow(
                            index = i + 1,
                            track = track,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            onClick = { onPlayTracks(ar.popularTracks, i) },
                        )
                    }
                }
                if (ar.albums.isNotEmpty()) {
                    item { SectionHeader("Albums", "") }
                    item { AlbumCarousel(ar.albums, onOpenAlbum) }
                }
            }
        }

        StickyBackBar(
            title = artist?.name ?: "",
            frosted = frosted,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private fun Artist.allTracks(): List<Track> = albums.flatMap { it.tracks }.ifEmpty { popularTracks }

@Composable
private fun ArtistHeader(artist: Artist, onPlay: () -> Unit, onShuffle: () -> Unit) {
    val palette = CoverPalette.from(artist.colorKey)
    Box(Modifier.fillMaxWidth().height(360.dp)) {
        // Full-bleed artist image (or gradient), fading into the page
        Box(Modifier.fillMaxSize()) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { Box(Modifier.fillMaxSize().background(palette.brush())) },
                    error = { Box(Modifier.fillMaxSize().background(palette.brush())) },
                )
            } else {
                Box(Modifier.fillMaxSize().background(palette.brush()))
            }
        }
        // gradient scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to BeatColors.Background,
                    )
                )
        )
        // name + actions pinned to the bottom of the header
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
        ) {
            Text(
                artist.name,
                style = BeatType.LargeTitle.copy(fontSize = 40.sp, letterSpacing = (-0.038f).em),
                color = BeatColors.TextPrimary,
            )
            Text(
                "${artist.trackCount} songs in your library",
                style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color(0xA6FFFFFF),
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Shuffle pill (outline)
                Box(
                    Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onShuffle
                        )
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle", tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Shuffle", style = BeatType.Pill, color = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                // 56dp gradient play
                Box(
                    Modifier
                        .size(56.dp)
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
    }
}
