package com.beatdrop.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.DownloadManager
import com.beatdrop.app.ui.components.LocalCancelDownload
import com.beatdrop.app.ui.components.LocalDownload
import com.beatdrop.app.ui.components.LocalIsLiked
import com.beatdrop.app.ui.components.LocalOpenProfile
import com.beatdrop.app.ui.components.LocalTrackMenu
import com.beatdrop.app.ui.components.MiniPlayer
import com.beatdrop.app.ui.components.TrackActions
import com.beatdrop.app.ui.components.TrackContextSheet
import com.beatdrop.app.ui.components.AddToPlaylistSheet
import com.beatdrop.app.ui.nav.Destination
import com.beatdrop.app.ui.nav.Dock
import com.beatdrop.app.ui.nav.Navigator
import com.beatdrop.app.ui.nav.Tab
import com.beatdrop.app.ui.screens.AlbumDetailScreen
import kotlinx.serialization.json.contentOrNull
import com.beatdrop.app.data.cloud.AuthManager
import com.beatdrop.app.data.cloud.AuthState
import com.beatdrop.app.ui.screens.AddScreen
import com.beatdrop.app.ui.screens.AuthScreen
import com.beatdrop.app.ui.screens.ArtistDetailScreen
import com.beatdrop.app.ui.screens.DownloadsScreen
import com.beatdrop.app.ui.screens.EqualizerScreen
import com.beatdrop.app.ui.screens.ProfileScreen
import com.beatdrop.app.ui.screens.SettingsScreen
import com.beatdrop.app.ui.screens.HomeScreen
import com.beatdrop.app.ui.screens.LibraryScreen
import com.beatdrop.app.ui.screens.LikedSongsScreen
import com.beatdrop.app.ui.screens.LikesViewModel
import com.beatdrop.app.ui.screens.LyricsScreen
import com.beatdrop.app.ui.screens.NotificationsAppViewModel
import com.beatdrop.app.ui.screens.NotificationsScreen
import com.beatdrop.app.ui.screens.NowPlayingScreen
import com.beatdrop.app.ui.screens.PlaybackViewModel
import com.beatdrop.app.ui.screens.PlaylistDetailScreen
import com.beatdrop.app.ui.screens.QueueScreen
import com.beatdrop.app.ui.screens.SearchScreen

@Composable
fun BeatDropApp() {
    val nav = remember { Navigator() }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    val playback: PlaybackViewModel = viewModel()
    val likes: LikesViewModel = viewModel()
    val auth: AuthManager = viewModel()
    val notifVm: NotificationsAppViewModel = viewModel()
    val unreadCount by notifVm.unread.collectAsStateWithLifecycle()
    val authState by auth.authState.collectAsStateWithLifecycle()
    val authLoading by auth.isLoading.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAuth by remember { mutableStateOf(false) }
    // Auto-dismiss the auth sheet once signed in.
    androidx.compose.runtime.LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                showAuth = false
                likes.syncOnSignIn()          // merge cloud likes + push local-first
                com.beatdrop.app.data.repository.MusicRepository(context).syncCloudPlaylists()
                notifVm.refresh()
                (authState as? AuthState.Authenticated)?.user?.id?.let { notifVm.listenRealtime(it) }
            }
            is AuthState.Unauthenticated -> {
                com.beatdrop.app.data.cloud.CloudSync.clearCache()
                notifVm.stopRealtime()
                notifVm.refresh()
            }
            else -> {}
        }
    }
    val signedInDisplayName = (authState as? AuthState.Authenticated)?.user?.userMetadata
        ?.get("display_name")
        ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
        ?.contentOrNull
    val pb by playback.state.collectAsStateWithLifecycle()
    val resolving by playback.resolving.collectAsStateWithLifecycle()
    val resolveError by playback.resolveError.collectAsStateWithLifecycle()
    val onlineDebugLines by playback.debugLog.collectAsStateWithLifecycle()
    val likedTracks by likes.liked.collectAsStateWithLifecycle()
    val downloadedTracks by DownloadManager.downloaded.collectAsStateWithLifecycle()
    val progress = if (pb.durationMs > 0) pb.positionMs.toFloat() / pb.durationMs else 0f

    // Track ⋯ context sheet + add-to-playlist sheet state
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var showSleepTimer by remember { mutableStateOf(false) }
    val sleepRemaining by com.beatdrop.app.player.SleepTimer.remainingMs.collectAsStateWithLifecycle()

    // Shared handlers
    val openAlbum: (Album) -> Unit = { nav.push(Destination.AlbumDetail(it.id)) }
    val openPlaylist: (String) -> Unit = { nav.push(Destination.PlaylistDetail(it)) }
    val playTracks: (List<Track>, Int) -> Unit = { tracks, i ->
        val tapped = tracks.getOrNull(i)
        if (tapped != null && tapped.id == pb.current?.id) {
            showNowPlaying = true
        } else {
            if (tapped?.source == MediaSource.ONLINE) showNowPlaying = true
            playback.play(tracks, i)
        }
    }

    val trackActions = remember(likedTracks) {
        TrackActions(
            onToggleLike = { likes.toggle(it) },
            onPlayNext = { playback.playNext(it) },
            onAddToQueue = { playback.addToQueue(it) },
            onAddToPlaylist = { addToPlaylistTrack = it },
            onGoToArtist = { nav.push(Destination.ArtistDetail(it.artist)) },
            onGoToAlbum = { if (it.album.isNotBlank()) nav.push(Destination.AlbumDetail(it.album)) },
            onShare = { t ->
                runCatching {
                    val text = "${t.title} — ${t.artist}"
                    context.startActivity(android.content.Intent.createChooser(
                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }, "Share"))
                }
            },
        )
    }

    CompositionLocalProvider(
        LocalTrackMenu provides { t: Track -> menuTrack = t },
        LocalIsLiked provides { id: String -> likedTracks.any { it.id == id } },
        LocalDownload provides { t: Track -> DownloadManager.enqueue(t, context) },
        LocalCancelDownload provides { id: String -> DownloadManager.cancel(id) },
        LocalOpenProfile provides { nav.push(Destination.Profile) },
    ) {
    Box(Modifier.fillMaxSize()) {
        // ── Tab host: current tab + its detail stack ──
        AnimatedContent(
            targetState = nav.currentTab to nav.currentDestination,
            transitionSpec = {
                // push = slide in from right; pop = slide back; tab switch = fade
                val (fromTab, fromDest) = initialState
                val (toTab, toDest) = targetState
                if (fromTab != toTab) {
                    fadeIn() togetherWith fadeOut()
                } else if (toDest != null && fromDest == null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith fadeOut()
                } else if (toDest == null && fromDest != null) {
                    fadeIn() togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith fadeOut()
                }
            },
            label = "navHost",
            modifier = Modifier.fillMaxSize(),
        ) { (tab, dest) ->
            when (dest) {
                is Destination.AlbumDetail -> AlbumDetailScreen(
                    albumId = dest.albumId,
                    currentTrackId = pb.current?.id,
                    isPlaying = pb.isPlaying,
                    onBack = { nav.pop() },
                    onPlayTracks = playTracks,
                    onOpenArtist = { nav.push(Destination.ArtistDetail(it)) },
                )
                is Destination.ArtistDetail -> ArtistDetailScreen(
                    artistName = dest.artistName,
                    currentTrackId = pb.current?.id,
                    isPlaying = pb.isPlaying,
                    onBack = { nav.pop() },
                    onPlayTracks = playTracks,
                    onOpenAlbum = openAlbum,
                )
                is Destination.PlaylistDetail -> PlaylistDetailScreen(
                    playlistId = dest.playlistId,
                    currentTrackId = pb.current?.id,
                    isPlaying = pb.isPlaying,
                    onBack = { nav.pop() },
                    onPlayTracks = playTracks,
                )
                Destination.LikedSongs -> LikedSongsScreen(
                    tracks = likedTracks,
                    currentTrackId = pb.current?.id,
                    isPlaying = pb.isPlaying,
                    onBack = { nav.pop() },
                    onPlayTracks = playTracks,
                )
                Destination.Downloads -> DownloadsScreen(
                    tracks = downloadedTracks,
                    usageLabel = formatBytes(DownloadManager.usageBytes(context)),
                    currentTrackId = pb.current?.id,
                    isPlaying = pb.isPlaying,
                    onBack = { nav.pop() },
                    onPlayTracks = playTracks,
                    onDelete = { DownloadManager.delete(it.id, context) },
                )
                Destination.Profile -> ProfileScreen(
                    likedCount = likedTracks.size,
                    downloadCount = downloadedTracks.size,
                    sleepActiveLabel = if (sleepRemaining > 0) "Ends in ${formatClock(sleepRemaining)}" else null,
                    isSignedIn = authState is AuthState.Authenticated,
                    displayName = signedInDisplayName,
                    email = (authState as? AuthState.Authenticated)?.user?.email,
                    onBack = { nav.pop() },
                    onSignIn = { showAuth = true },
                    onSignOut = { auth.signOut() },
                    onOpenSettings = { nav.push(Destination.Settings) },
                    onOpenEqualizer = { nav.push(Destination.Equalizer) },
                    onOpenLiked = { nav.push(Destination.LikedSongs) },
                    onOpenDownloads = { nav.push(Destination.Downloads) },
                    onOpenSleepTimer = { showSleepTimer = true },
                    onOpenNotifications = { nav.push(Destination.Notifications) },
                    unreadCount = unreadCount,
                )
                Destination.Notifications -> NotificationsScreen(onBack = { nav.pop() }, vm = notifVm)
                Destination.Settings -> SettingsScreen(onBack = { nav.pop() })
                Destination.Equalizer -> EqualizerScreen(onBack = { nav.pop() })
                null -> TabRoot(
                    tab = tab,
                    pb = pb,
                    openAlbum = openAlbum,
                    openPlaylist = openPlaylist,
                    openLiked = { nav.push(Destination.LikedSongs) },
                    openDownloads = { nav.push(Destination.Downloads) },
                    playTracks = playTracks,
                    onSearch = { nav.selectTab(Tab.Search) },
                    onAdd = { nav.selectTab(Tab.Add) },
                    onOpenArtist = { nav.push(Destination.ArtistDetail(it)) },
                    onSignIn = { showAuth = true },
                    isSignedIn = authState is AuthState.Authenticated,
                    displayName = signedInDisplayName,
                )
            }
        }

        // ── Mini player ──
        MiniPlayer(
            track = pb.current,
            isPlaying = pb.isPlaying,
            progress = progress,
            onClick = { showNowPlaying = true },
            onTogglePlay = { playback.togglePlayPause() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 100.dp),
        )

        // ── Dock ──
        Dock(
            selected = nav.currentTab,
            onSelect = { nav.selectTab(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 22.dp)
        )

        // Online stream resolution is reflected inside Now Playing; no blocking spinner overlay.

        // ── Now Playing overlay ──
        AnimatedVisibility(
            visible = showNowPlaying && pb.current != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            pb.current?.let { track ->
                NowPlayingScreen(
                    track = track,
                    isPlaying = pb.isPlaying,
                    positionMs = pb.positionMs,
                    durationMs = pb.durationMs,
                    onClose = { showNowPlaying = false },
                    onTogglePlay = { playback.togglePlayPause() },
                    onNext = { playback.next() },
                    onPrevious = { playback.previous() },
                    onSeek = { playback.seekTo(it) },
                    onOpenLyrics = { showLyrics = true },
                    onOpenQueue = { showQueue = true },
                    isLiked = likedTracks.any { it.id == track.id },
                    isResolving = resolving && track.source == MediaSource.ONLINE,
                    resolveError = resolveError,
                    debugLogText = onlineDebugLines.joinToString("\n"),
                    onRetry = { playback.retryOnline() },
                    onToggleLike = { likes.toggle(track) },
                )
            }
            BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
        }

        // ── Lyrics overlay ──
        AnimatedVisibility(
            visible = showLyrics && pb.current != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            pb.current?.let { track ->
                LyricsScreen(
                    track = track,
                    isPlaying = pb.isPlaying,
                    positionMs = pb.positionMs,
                    durationMs = pb.durationMs,
                    onClose = { showLyrics = false },
                    onTogglePlay = { playback.togglePlayPause() },
                    onSeek = { playback.seekTo(it) },
                )
            }
            BackHandler(enabled = showLyrics) { showLyrics = false }
        }

        // ── Queue overlay (on top of Now Playing) ──
        AnimatedVisibility(
            visible = showQueue && pb.current != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            QueueScreen(
                pb = pb,
                onClose = { showQueue = false },
                onJumpTo = { playback.jumpTo(it) },
                onMove = { from, to -> playback.moveQueueItem(from, to) },
                onRemove = { playback.removeQueueItem(it) },
                onToggleShuffle = { playback.toggleShuffle() },
                onCycleRepeat = { playback.cycleRepeat() },
            )
            BackHandler(enabled = showQueue) { showQueue = false }
        }

        // ── Global track ⋯ context sheet ──
        menuTrack?.let { t ->
            TrackContextSheet(
                track = t,
                isLiked = likedTracks.any { it.id == t.id },
                actions = trackActions,
                onDismiss = { menuTrack = null },
            )
        }

        // ── Add-to-playlist sheet (from the ⋯ menu) ──
        addToPlaylistTrack?.let { t ->
            AddToPlaylistSheet(
                track = t,
                onDismiss = { addToPlaylistTrack = null },
            )
        }

        // ── Auth overlay (optional sign-in for cloud sync) ──
        AnimatedVisibility(
            visible = showAuth,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            AuthScreen(
                state = authState,
                isLoading = authLoading,
                onSignIn = { e, p -> auth.signIn(e, p) },
                onSignUp = { e, p, dn, un -> auth.signUp(e, p, dn, un) },
                onReset = { e -> auth.sendPasswordReset(e) },
                onClose = { showAuth = false },
            )
            BackHandler(enabled = showAuth) { showAuth = false }
        }

        // ── Sleep timer dialog ──
        if (showSleepTimer) {
            SleepTimerDialog(
                remainingMs = sleepRemaining,
                onPick = { com.beatdrop.app.player.SleepTimer.start(it); showSleepTimer = false },
                onCancel = { com.beatdrop.app.player.SleepTimer.cancel(); showSleepTimer = false },
                onDismiss = { showSleepTimer = false },
            )
        }

        // System back: pop detail stack when no overlay is open
        BackHandler(enabled = !showNowPlaying && !showLyrics && !showQueue && nav.canPop) { nav.pop() }
    }
    }
}

@Composable
private fun TabRoot(
    tab: Tab,
    pb: com.beatdrop.app.player.PlaybackState,
    openAlbum: (Album) -> Unit,
    openPlaylist: (String) -> Unit,
    openLiked: () -> Unit,
    openDownloads: () -> Unit,
    playTracks: (List<Track>, Int) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onSignIn: () -> Unit,
    isSignedIn: Boolean,
    displayName: String?,
) {
    when (tab) {
        Tab.Home -> HomeScreen(
            onOpenAlbum = openAlbum,
            onOpenLiked = openLiked,
            onOpenDownloads = openDownloads,
            onSearch = onSearch,
            onAdd = onAdd,
            onPlayTracks = playTracks,
            currentTrackId = pb.current?.id,
            isPlaying = pb.isPlaying,
            displayName = displayName,
            isSignedIn = isSignedIn,
            onSignIn = onSignIn,
        )
        Tab.Search -> SearchScreen(
            currentTrackId = pb.current?.id,
            isPlaying = pb.isPlaying,
            onOpenAlbum = openAlbum,
            onOpenArtist = onOpenArtist,
            onPlayTracks = playTracks,
            onAdd = onAdd,
        )
        Tab.Library -> LibraryScreen(
            currentTrackId = pb.current?.id,
            isPlaying = pb.isPlaying,
            onOpenAlbum = openAlbum,
            onOpenLiked = openLiked,
            onOpenDownloads = openDownloads,
            onPlayTracks = playTracks,
            onSearch = onSearch,
            onAdd = onAdd,
            displayName = displayName,
            isSignedIn = isSignedIn,
            onSignIn = onSignIn,
        )
        Tab.Add -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            AddScreen(
                onImportFromDevice = {
                    // Real action: open the system audio picker so users can add music.
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                type = "audio/*"
                                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                            }
                        )
                    }
                },
                onOpenPlaylist = { openPlaylist(it.id) },
                onSearch = onSearch,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.0f MB".format(mb)
}

private fun formatClock(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun SleepTimerDialog(
    remainingMs: Long,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(5, 10, 15, 30, 45, 60)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color(0xFF15121A),
        title = {
            androidx.compose.material3.Text(
                "Sleep timer",
                color = com.beatdrop.app.ui.theme.BeatColors.TextPrimary,
                style = com.beatdrop.app.ui.theme.BeatType.SectionTitle
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                if (remainingMs > 0) {
                    androidx.compose.material3.Text(
                        "Active — ends in ${formatClock(remainingMs)}",
                        color = com.beatdrop.app.ui.theme.BeatColors.Accent,
                        style = com.beatdrop.app.ui.theme.BeatType.CardSub,
                    )
                }
                options.forEach { min ->
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(min) }
                            .padding(vertical = 10.dp)
                    ) {
                        androidx.compose.material3.Text(
                            "$min minutes",
                            color = com.beatdrop.app.ui.theme.BeatColors.TextPrimary,
                            style = com.beatdrop.app.ui.theme.BeatType.TrackTitle
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                androidx.compose.material3.Text("Turn off", color = com.beatdrop.app.ui.theme.BeatColors.Accent)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Close", color = com.beatdrop.app.ui.theme.BeatColors.TextSecondary)
            }
        }
    )
}
