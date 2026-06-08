package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.QuickItem
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.DownloadManager
import com.beatdrop.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val isEmpty: Boolean = false,
    val quick: List<QuickItem> = emptyList(),
    val recent: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList(),
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state

    init {
        // Re-merge downloads whenever a new download completes.
        viewModelScope.launch {
            DownloadManager.downloaded.collect { dl ->
                val s = _state.value
                if (!s.loading) {
                    val merged = (dl + s.tracks).distinctBy { it.id }
                    _state.value = s.copy(tracks = merged, isEmpty = merged.isEmpty())
                }
            }
        }
    }

    fun load() {
        if (!_state.value.loading && _state.value.tracks.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val albums = repo.allAlbums()
            val deviceTracks = repo.allTracks()
            val downloads = DownloadManager.downloaded.value
            val tracks = (downloads + deviceTracks).distinctBy { it.id }
            val quick = buildList {
                add(QuickItem("Liked Songs", "cover-liked", "Liked Songs"))
                add(QuickItem("Downloads", "c-5", "Downloads"))
                albums.take(4).forEach { add(QuickItem(it.title, it.colorKey ?: "c-1", it.id)) }
            }
            _state.value = LibraryUiState(
                loading = false,
                isEmpty = tracks.isEmpty(),
                quick = quick,
                recent = albums.take(10),
                tracks = tracks,
            )
        }
    }
}
