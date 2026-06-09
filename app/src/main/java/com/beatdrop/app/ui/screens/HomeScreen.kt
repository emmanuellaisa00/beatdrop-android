package com.beatdrop.app.ui.screens

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.ui.components.AddRow
import com.beatdrop.app.ui.components.CompactHeader
import com.beatdrop.app.ui.components.HeaderIcon
import com.beatdrop.app.ui.components.Hero
import com.beatdrop.app.ui.components.ScreenBackground
import com.beatdrop.app.ui.components.ScreenTheme
import com.beatdrop.app.ui.components.SectionHeader
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush

/**
 * Home is the online/cloud Spotify-style entry point. Local/on-device music lives
 * in Library. Home is still local-first safe: if signed out, it explains why
 * cloud helps and keeps navigation to Search available.
 */
@Composable
fun HomeScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    currentTrackId: String? = null,
    isPlaying: Boolean = false,
    displayName: String? = null,
    isSignedIn: Boolean = false,
    onSignIn: () -> Unit = {},
) {
    val context = LocalContext.current
    var onboardingDone by remember {
        mutableStateOf(
            context.getSharedPreferences("beatdrop_cloud_library", Context.MODE_PRIVATE)
                .getBoolean("artists_done", false)
        )
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground(ScreenTheme.Home)
        when {
            !isSignedIn -> OnlineHomeGate(onSignIn = onSignIn, onSearch = onSearch)
            !onboardingDone -> ArtistSeedOnboarding(
                displayName = displayName,
                onDone = { artists ->
                    context.getSharedPreferences("beatdrop_cloud_library", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("artists_done", true)
                        .putStringSet("favorite_artists", artists)
                        .apply()
                    onboardingDone = true
                }
            )
            else -> OnlineHomeContent(
                displayName = displayName,
                onSearch = onSearch,
                onOpenLiked = onOpenLiked,
                onOpenDownloads = onOpenDownloads,
                onAdd = onAdd,
            )
        }
    }
}

@Composable
private fun OnlineHomeGate(onSignIn: () -> Unit, onSearch: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedCloudIcon()
        Spacer(Modifier.height(20.dp))
        Text("BeatDrop online", style = BeatType.LargeTitle, color = BeatColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Sign in to sync likes, playlists, recent plays, artist picks, and suggestions. You can still search and play online music without breaking local playback.",
            style = BeatType.CardSub,
            color = BeatColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "By continuing you agree to BeatDrop terms and conditions from LAISACORP.",
            style = BeatType.TrackSub,
            color = BeatColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Sign in", Icons.Rounded.PersonAdd, onSignIn)
            PillButton("Search", Icons.Rounded.Search, onSearch)
        }
    }
}

@Composable
private fun AnimatedCloudIcon() {
    val transition = rememberInfiniteTransition(label = "homeCloud")
    val scale by transition.animateFloat(
        0.96f,
        1.06f,
        infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "cloudScale",
    )
    Box(
        Modifier
            .size(86.dp)
            .scale(scale)
            .clip(RoundedCornerShape(30.dp))
            .background(PillActiveBrush)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.CloudDone, null, tint = Color.White, modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun PillButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(PillActiveBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, style = BeatType.Pill, color = Color.White)
    }
}

@Composable
private fun ArtistSeedOnboarding(displayName: String?, onDone: (Set<String>) -> Unit) {
    val artists = remember {
        listOf(
            "Drake", "Taylor Swift", "Burna Boy", "SZA", "The Weeknd", "Billie Eilish",
            "Wizkid", "Ariana Grande", "Kendrick Lamar", "Rema", "Tems", "Travis Scott",
            "Adele", "Bad Bunny", "Post Malone", "Doja Cat", "Future", "Ayra Starr"
        )
    }
    var selected by remember { mutableStateOf(setOf<String>()) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 86.dp, start = 20.dp, end = 20.dp, bottom = 190.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(PillActiveBrush),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Star, null, tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (displayName.isNullOrBlank()) "Choose your artists" else "Choose artists, $displayName",
                        style = BeatType.SectionTitle,
                        color = BeatColors.TextPrimary,
                    )
                    Text("Pick at least 3 for better suggestions", style = BeatType.CardSub, color = BeatColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        items(artists.size) { idx ->
            val artist = artists[idx]
            val active = artist in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) BeatColors.AccentDim else BeatColors.Surface)
                    .border(1.dp, if (active) BeatColors.Accent else BeatColors.GlassBorder, RoundedCornerShape(20.dp))
                    .clickable { selected = if (active) selected - artist else selected + artist }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(BeatColors.SurfaceHover), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = BeatColors.Accent, modifier = Modifier.size(18.dp))
                    }
                    Text(artist, style = BeatType.TrackTitle, color = BeatColors.TextPrimary)
                }
                Text(if (active) "Selected" else "Add", style = BeatType.Pill, color = if (active) BeatColors.Accent else BeatColors.TextSecondary)
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (selected.size >= 3) PillActiveBrush else Brush.linearGradient(listOf(BeatColors.SurfaceHover, BeatColors.SurfaceHover)))
                    .clickable(enabled = selected.size >= 3) { onDone(selected) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (selected.size >= 3) "Continue" else "Choose ${3 - selected.size} more",
                    style = BeatType.Pill,
                    color = if (selected.size >= 3) Color.White else BeatColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun OnlineHomeContent(
    displayName: String?,
    onSearch: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val favoriteArtists = remember {
        context.getSharedPreferences("beatdrop_cloud_library", Context.MODE_PRIVATE)
            .getStringSet("favorite_artists", emptySet())
            .orEmpty()
            .toList()
            .sorted()
    }
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 200.dp),
        ) {
            item {
                Hero(
                    greetingPlain = if (displayName.isNullOrBlank()) "Online" else "Online,",
                    greetingBold = displayName?.takeIf { it.isNotBlank() },
                    titlePlain = "For",
                    titleAccent = "you",
                    avatarText = displayName?.take(1),
                    onSearch = onSearch,
                    onAdd = onAdd,
                )
            }
            item { SectionHeader("Start listening", "") }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddRow(Icons.Rounded.Search, "Search online music", "Songs, albums, playlists and artists") { onSearch() }
                    AddRow(Icons.Rounded.CloudDone, "Cloud likes", "Synced liked songs when signed in") { onOpenLiked() }
                    AddRow(Icons.Rounded.LibraryMusic, "Downloads", "Offline music stays available on this device") { onOpenDownloads() }
                }
            }
            item { SectionHeader("Artist seeds", "") }
            item {
                Text(
                    if (favoriteArtists.isEmpty()) {
                        "Choose artists to unlock better online suggestions."
                    } else {
                        favoriteArtists.joinToString("  •  ")
                    },
                    style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold),
                    color = BeatColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        CompactHeader(
            title = "Home",
            listState = listState,
            icons = listOf(HeaderIcon(Icons.Rounded.Search, "Search", onSearch)),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
