package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.model.QuickItem
import com.beatdrop.app.data.model.Shelf
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val hasPermission: Boolean = false,
    val isEmpty: Boolean = false,
    val quick: List<QuickItem> = emptyList(),
    val shelves: List<Shelf> = emptyList(),
    val tracks: List<Track> = emptyList(),
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun onPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(hasPermission = granted)
        if (granted) load()
        else _state.value = _state.value.copy(loading = false)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val shelves = repo.homeShelves()
            val tracks = repo.allTracks()
            val albums = shelves.firstOrNull()?.items.orEmpty()
            val quick = buildList {
                add(QuickItem("Liked Songs", "cover-liked", "Liked Songs"))
                add(QuickItem("Downloads", "c-5", "Downloads"))
                albums.take(4).forEach { add(QuickItem(it.title, it.colorKey ?: "c-1", it.id)) }
            }
            _state.value = HomeUiState(
                loading = false,
                hasPermission = true,
                isEmpty = shelves.isEmpty(),
                quick = quick,
                shelves = shelves,
                tracks = tracks,
            )
        }
    }
}
