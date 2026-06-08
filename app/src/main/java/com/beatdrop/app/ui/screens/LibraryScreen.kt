package com.beatdrop.app.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
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
 * Library is cloud/online only. Local music lives on Home.
 * First access explains cloud sync + LAISACORP terms, then signed-in users pick
 * at least 3 favorite artists for future suggestions. Artist choices are local
 * for now; no database migration is required unless you want cross-device sync.
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
        ScreenBackground(ScreenTheme.Library)
        when {
            !isSignedIn -> CloudLibraryGate(onSignIn = onSignIn)
            !onboardingDone -> ArtistPicker(
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
            else -> OnlineLibraryHome(displayName = displayName, onSearch = onSearch, onOpenLiked = onOpenLiked, onOpenDownloads = onOpenDownloads)
        }
    }
}

@Composable
private fun CloudLibraryGate(onSignIn: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(PillActiveBrush)
                .padding(18.dp),
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.CloudDone, null, tint = Color.White)
        }
        Spacer(Modifier.height(18.dp))
        Text("Your online Library", style = BeatType.LargeTitle, color = BeatColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Sign in to save your cloud library, likes, playlists, and artist suggestions. BeatDrop by LAISACORP keeps local playback working even when cloud sync is unavailable.",
            style = BeatType.CardSub,
            color = BeatColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "By continuing you agree to BeatDrop terms and conditions from LAISACORP.",
            style = BeatType.TrackSub,
            color = Color(0x8CFFFFFF),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(PillActiveBrush)
                .clickable(onClick = onSignIn)
                .padding(horizontal = 26.dp, vertical = 13.dp),
        ) {
            Text("Sign in / create account", style = BeatType.Pill, color = Color.White)
        }
    }
}

@Composable
private fun ArtistPicker(displayName: String?, onDone: (Set<String>) -> Unit) {
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
        contentPadding = PaddingValues(top = 90.dp, start = 20.dp, end = 20.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (displayName.isNullOrBlank()) "Choose your artists" else "Choose artists, $displayName",
                style = BeatType.LargeTitle,
                color = BeatColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick at least 3 artists so BeatDrop can shape your online Library suggestions.",
                style = BeatType.CardSub,
                color = BeatColors.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
        }
        items(artists.size) { idx ->
            val artist = artists[idx]
            val active = artist in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Color(0x22FF375F) else Color(0x14FFFFFF))
                    .border(1.dp, if (active) BeatColors.Accent else Color(0x17FFFFFF), RoundedCornerShape(18.dp))
                    .clickable {
                        selected = if (active) selected - artist else selected + artist
                    }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(artist, style = BeatType.TrackTitle, color = BeatColors.TextPrimary)
                Text(if (active) "Selected" else "Add", style = BeatType.Pill, color = if (active) BeatColors.Accent else BeatColors.TextSecondary)
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selected.size >= 3) PillActiveBrush else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x22FFFFFF))))
                    .clickable(enabled = selected.size >= 3) { onDone(selected) }
                    .padding(vertical = 14.dp),
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
private fun OnlineLibraryHome(
    displayName: String?,
    onSearch: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
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
                    greetingPlain = if (displayName.isNullOrBlank()) "Cloud Library" else "Cloud Library,",
                    greetingBold = displayName?.takeIf { it.isNotBlank() },
                    titlePlain = "Your online",
                    titleAccent = "Library",
                    showActions = false,
                )
            }
            item { SectionHeader("Start here", "") }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddRow(Icons.Rounded.Search, "Search online music", "Songs, albums, playlists and artists") { onSearch() }
                    AddRow(Icons.Rounded.CloudDone, "Cloud likes", "Synced liked songs when signed in") { onOpenLiked() }
                    AddRow(Icons.Rounded.CloudDone, "Downloads", "Offline music stays available on this device") { onOpenDownloads() }
                }
            }
            item { SectionHeader("Suggestions", "Coming soon") }
            item {
                Text(
                    if (favoriteArtists.isEmpty()) {
                        "Your artist picks are saved locally for now. If you want this to sync across devices, add a Supabase favorite-artists table later."
                    } else {
                        "Artist seeds: ${favoriteArtists.joinToString(", ")}\n\nSaved locally for now. To sync across devices, add a Supabase favorite-artists table later."
                    },
                    style = BeatType.CardSub.copy(fontWeight = FontWeight.Medium),
                    color = BeatColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        CompactHeader(
            title = "Library",
            listState = listState,
            icons = listOf(HeaderIcon(Icons.Rounded.Search, "Search", onSearch)),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
