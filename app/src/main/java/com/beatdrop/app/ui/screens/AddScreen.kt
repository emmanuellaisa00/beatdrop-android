package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beatdrop.app.data.local.UserPlaylist
import com.beatdrop.app.data.repository.MusicRepository
import com.beatdrop.app.ui.components.AddRow
import com.beatdrop.app.ui.components.CompactHeader
import com.beatdrop.app.ui.components.HeaderIcon
import com.beatdrop.app.ui.components.Hero
import com.beatdrop.app.ui.components.ScreenBackground
import com.beatdrop.app.ui.components.ScreenTheme
import com.beatdrop.app.ui.components.SectionHeader
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val _playlists = MutableStateFlow(repo.userPlaylists())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists

    fun refresh() { _playlists.value = repo.userPlaylists() }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val playlist = repo.createPlaylist(name.ifBlank { "New Playlist" })
            repo.pushPlaylistMetadata(playlist)
            _playlists.value = repo.userPlaylists()
        }
    }
}

/**
 * Add / Create — ported from #screen-add. Hero ("What's new?"), the action list
 * (Create a Playlist / Blend / Scan / Paste link / Import), and a Recent activity
 * list showing the user's real created playlists.
 */
@Composable
fun AddScreen(
    onImportFromDevice: () -> Unit,
    onOpenPlaylist: (UserPlaylist) -> Unit,
    onSearch: () -> Unit,
    vm: AddViewModel = viewModel(),
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground(ScreenTheme.Add)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 200.dp)
        ) {
            item {
                Hero(
                    greetingPlain = "Create",
                    titlePlain = "What's",
                    titleAccent = "new?",
                    showActions = false,
                )
            }
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AddRow(Icons.Rounded.Add, "Create a Playlist", "Start fresh with an empty playlist") {
                        showCreateDialog = true
                    }
                    AddRow(Icons.Rounded.Group, "Blend with a friend", "Make a shared playlist that updates daily") {
                        showCreateDialog = true
                    }
                    AddRow(Icons.Rounded.QrCodeScanner, "Scan a Code", "Use your camera to import a track") {
                        onImportFromDevice()
                    }
                    AddRow(Icons.Rounded.Link, "Paste a Link", "YouTube, SoundCloud, or any URL") {
                        showPasteDialog = true
                    }
                    AddRow(Icons.Rounded.Upload, "Import from Device", "Add music files from your phone") {
                        onImportFromDevice()
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item { SectionHeader("Recent activity", "") }
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        playlists.forEach { p ->
                            AddRow(
                                icon = Icons.Rounded.Add,
                                title = p.name,
                                description = "${p.trackIds.size} songs",
                                iconBrush = Brush.linearGradient(
                                    listOf(Color(0xFF2A6F97), Color(0xFF014F86))
                                ),
                                onClick = { onOpenPlaylist(p) }
                            )
                        }
                    }
                }
            }
        }

        CompactHeader(
            title = "Create",
            listState = listState,
            icons = listOf(HeaderIcon(Icons.Rounded.Add, "Search", onSearch)),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showPasteDialog) {
        PasteLinkDialog(
            onDismiss = { showPasteDialog = false },
            onSearch = {
                showPasteDialog = false
                onSearch()
            },
            onOpenUrl = { url ->
                runCatching {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }
                showPasteDialog = false
            }
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                vm.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}


@Composable
private fun PasteLinkDialog(
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var link by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15121A),
        title = { Text("Paste a link", color = BeatColors.TextPrimary, style = BeatType.SectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Paste a YouTube, SoundCloud, or music link. BeatDrop can open it externally now, or you can search for it in-app.",
                    color = BeatColors.TextSecondary,
                    style = BeatType.CardSub,
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    singleLine = true,
                    placeholder = { Text("https://...", color = BeatColors.TextMuted) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (link.isNotBlank()) onOpenUrl(link.trim()) else onSearch() }) {
                Text(if (link.isBlank()) "Search" else "Open", color = BeatColors.Accent)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSearch) { Text("Search", color = BeatColors.TextSecondary) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = BeatColors.TextSecondary) }
            }
        }
    )
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15121A),
        title = { Text("New playlist", color = BeatColors.TextPrimary, style = BeatType.SectionTitle) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Playlist name", color = BeatColors.TextMuted) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }) {
                Text("Create", color = BeatColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = BeatColors.TextSecondary) }
        }
    )
}
