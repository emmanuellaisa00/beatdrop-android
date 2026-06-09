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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import coil.compose.SubcomposeAsyncImage
import com.beatdrop.app.ui.components.TrackRow
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.CoverPalette
import com.beatdrop.app.ui.theme.PillActiveBrush
import com.beatdrop.app.ui.theme.brush
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbumDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album
    fun load(id: String) {
        viewModelScope.launch { _album.value = repo.albumById(id) }
    }
}

/**
 * Album / Playlist detail — ported from #screen-album.
 * Blurred cover backdrop, 210dp cover, title 25/900, meta, action row
 * (like/download/share/shuffle + 58dp gradient play), and the full track list.
 * Sticky-back bar frosts + reveals the title once scrolled (HTML behavior).
 */
@Composable
fun AlbumDetailScreen(
    albumId: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenArtist: (String) -> Unit,
    vm: AlbumDetailViewModel = viewModel(),
) {
    LaunchedEffect(albumId) { vm.load(albumId) }
    val album by vm.album.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    BackHandler(onBack = onBack)

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
            var liked by remember { mutableStateOf(false) }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 200.dp)
            ) {
                item { AlbumHero(a, onOpenArtist = { onOpenArtist(a.artist) }) }
                item {
                    AlbumActions(
                        liked = liked,
                        onLike = { liked = !liked },
                        onShuffle = {
                            if (a.tracks.isNotEmpty()) {
                                val shuffled = a.tracks.shuffled()
                                onPlayTracks(shuffled, 0)
                            }
                        },
                        onPlay = { if (a.tracks.isNotEmpty()) onPlayTracks(a.tracks, 0) },
                        onDownload = {
                            a.tracks.forEach { com.beatdrop.app.data.online.DownloadManager.enqueue(it, context) }
                        },
                        onShare = {
                            runCatching {
                                context.startActivity(android.content.Intent.createChooser(
                                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, "${a.title} — ${a.artist}")
                                    }, "Share album"))
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
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

        StickyBackBar(
            title = album?.title ?: "",
            frosted = frosted,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun AlbumHero(album: Album, onOpenArtist: () -> Unit) {
    val palette = CoverPalette.from(album.colorKey)
    Box(Modifier.fillMaxWidth()) {
        // Apple Music-style artwork backdrop: the cover fills the top, heavily blurred,
        // then fades into the dark page. Falls back to BeatDrop cover gradients.
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .scale(1.28f)
                .alpha(0.82f)
                .blur(64.dp)
                .background(palette.brush())
        ) {
            album.artworkUri?.let { art ->
                SubcomposeAsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(Color(0x66000000))
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x22000000),
                        0.55f to Color(0x88000000),
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
            CoverArt(
                album.artworkUri, album.colorKey,
                size = 210.dp, corner = 16.dp, glyphSize = 76.dp
            )
            Text(
                album.title,
                style = BeatType.LargeTitle.copy(fontSize = 25.sp, letterSpacing = (-0.030f).em),
                color = BeatColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 22.dp)
            )
            Text(
                "${album.artist} · ${if (album.isPlaylist) "Playlist" else "Album"}",
                style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color(0xA6FFFFFF),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onOpenArtist
                    )
            )
            Text(
                buildStats(album),
                style = BeatType.CardSub.copy(fontSize = 12.sp),
                color = Color(0x6BFFFFFF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun buildStats(album: Album): String {
    val totalMs = album.tracks.sumOf { it.durationMs }
    val mins = (totalMs / 60000).toInt()
    val year = album.year?.let { "$it · " } ?: ""
    return "$year${album.tracks.size} songs · $mins min"
}

@Composable
private fun AlbumActions(
    liked: Boolean,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionIcon(
            if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            "Like", tint = if (liked) BeatColors.Accent else Color(0xE0FFFFFF), onClick = onLike
        )
        ActionIcon(Icons.Rounded.Download, "Download", onClick = onDownload)
        ActionIcon(Icons.Rounded.Share, "Share", onClick = onShare)
        ActionIcon(Icons.Rounded.Shuffle, "Shuffle", onClick = onShuffle)
        Spacer(Modifier.weight(1f))
        // 58dp gradient play-big
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
private fun ActionIcon(icon: ImageVector, desc: String, tint: Color = Color(0xE0FFFFFF), onClick: () -> Unit) {
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
        Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}
