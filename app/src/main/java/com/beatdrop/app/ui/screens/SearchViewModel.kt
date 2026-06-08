package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.YoutubeService
import com.beatdrop.app.data.online.toAlbum
import com.beatdrop.app.data.online.toTrack
import com.beatdrop.app.data.repository.MusicRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class SearchScope { LOCAL, ONLINE }

data class SearchUiState(
    val query: String = "",
    val scope: SearchScope = SearchScope.ONLINE,
    val loading: Boolean = false,
    val resultsTracks: List<Track> = emptyList(),
    val resultsAlbums: List<Album> = emptyList(),
    val resultsPlaylists: List<Album> = emptyList(),
    val hasQuery: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private var allTracks: List<Track> = emptyList()
    private var allAlbums: List<Album> = emptyList()

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            allTracks = repo.allTracks()
            allAlbums = repo.allAlbums()
        }
        viewModelScope.launch {
            _query.debounce(300).distinctUntilChanged().collect { q -> runSearch(q) }
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, hasQuery = q.isNotBlank())
        _query.value = q
    }

    fun setScope(scope: SearchScope) {
        if (scope == _state.value.scope) return
        _state.value = _state.value.copy(scope = scope)
        runSearch(_state.value.query)
    }

    fun clear() {
        searchJob?.cancel()
        _state.value = SearchUiState(scope = _state.value.scope)
        _query.value = ""
    }

    private fun runSearch(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.value = _state.value.copy(
                resultsTracks = emptyList(), resultsAlbums = emptyList(),
                resultsPlaylists = emptyList(), hasQuery = false, loading = false
            )
            return
        }
        when (_state.value.scope) {
            SearchScope.LOCAL -> runLocal(q)
            SearchScope.ONLINE -> runOnline(q)
        }
    }

    private fun runLocal(q: String) {
        val needle = q.trim().lowercase()
        val tracks = allTracks.filter {
            it.title.lowercase().contains(needle) ||
                it.artist.lowercase().contains(needle) ||
                it.album.lowercase().contains(needle)
        }.take(40)
        val albums = allAlbums.filter {
            it.title.lowercase().contains(needle) || it.artist.lowercase().contains(needle)
        }.take(10)
        _state.value = _state.value.copy(
            resultsTracks = tracks, resultsAlbums = albums,
            resultsPlaylists = emptyList(), hasQuery = true, loading = false
        )
    }

    private fun runOnline(q: String) {
        _state.value = _state.value.copy(loading = true, hasQuery = true)
        searchJob = viewModelScope.launch {
            val res = YoutubeService.searchAll(q)
            // Ignore stale responses if the query changed meanwhile.
            if (_state.value.query.trim() != q.trim()) return@launch
            _state.value = _state.value.copy(
                loading = false,
                resultsTracks = res.songs.map { it.toTrack() },
                resultsAlbums = res.albums.map { it.toAlbum() },
                resultsPlaylists = res.playlists.map { it.toAlbum() },
            )
        }
    }
}
