package com.beatdrop.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.YoutubeService
import com.beatdrop.app.data.online.toAlbum
import com.beatdrop.app.data.online.toTrack
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class SearchCategory { SONGS, ALBUMS, PLAYLISTS, ARTISTS }

data class SearchUiState(
    val query: String = "",
    val category: SearchCategory = SearchCategory.SONGS,
    val loading: Boolean = false,
    val resultsTracks: List<Track> = emptyList(),
    val resultsAlbums: List<Album> = emptyList(),
    val resultsPlaylists: List<Album> = emptyList(),
    val resultsArtists: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val hasQuery: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("beatdrop_search", Context.MODE_PRIVATE)
    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState(recentSearches = loadRecent()))
    val state: StateFlow<SearchUiState> = _state

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _query.debounce(300).distinctUntilChanged().collect { q -> runOnline(q) }
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, hasQuery = q.isNotBlank())
        _query.value = q
    }

    fun setCategory(category: SearchCategory) {
        if (category == _state.value.category) return
        _state.value = _state.value.copy(category = category)
        runOnline(_state.value.query)
    }

    fun clear() {
        searchJob?.cancel()
        _state.value = SearchUiState(category = _state.value.category, recentSearches = _state.value.recentSearches)
        _query.value = ""
    }

    fun clearRecent() {
        prefs.edit().remove(KEY_RECENT).apply()
        _state.value = _state.value.copy(recentSearches = emptyList())
    }

    private fun runOnline(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.value = _state.value.copy(
                resultsTracks = emptyList(),
                resultsAlbums = emptyList(),
                resultsPlaylists = emptyList(),
                resultsArtists = emptyList(),
                hasQuery = false,
                loading = false,
            )
            return
        }
        _state.value = _state.value.copy(loading = true, hasQuery = true)
        searchJob = viewModelScope.launch {
            val res = YoutubeService.searchAll(q)
            if (_state.value.query.trim() != q.trim()) return@launch
            val tracks = res.songs.map { it.toTrack() }
            val albums = res.albums.map { it.toAlbum() }
            val playlists = res.playlists.map { it.toAlbum() }
            val artists = (tracks.map { it.artist } + albums.map { it.artist })
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("Unknown artist", ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .take(20)
            val recent = saveRecent(q)
            _state.value = _state.value.copy(
                loading = false,
                resultsTracks = tracks,
                resultsAlbums = albums,
                resultsPlaylists = playlists,
                resultsArtists = artists,
                recentSearches = recent,
            )
        }
    }

    private fun loadRecent(): List<String> = prefs.getString(KEY_RECENT, "")
        .orEmpty()
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    private fun saveRecent(query: String): List<String> {
        val q = query.trim()
        if (q.isBlank()) return _state.value.recentSearches
        val next = (listOf(q) + _state.value.recentSearches)
            .distinctBy { it.lowercase() }
            .take(8)
        prefs.edit().putString(KEY_RECENT, next.joinToString("\n")).apply()
        return next
    }

    companion object { private const val KEY_RECENT = "recent_queries" }
}
