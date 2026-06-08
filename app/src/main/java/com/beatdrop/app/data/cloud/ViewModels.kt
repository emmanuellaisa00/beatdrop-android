package com.beatdrop.app.data.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════
// PLAYLIST VIEWMODEL
// ═══════════════════════════════════════════════════
class PlaylistViewModel(
    private val repo: PlaylistRepository = PlaylistRepository(),
    private val songsRepo: PlaylistSongsRepository = PlaylistSongsRepository()
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadMyPlaylists() }

    fun loadMyPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _playlists.value = repo.getMyPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(title: String, description: String? = null, isPublic: Boolean = false) {
        viewModelScope.launch {
            try {
                repo.createPlaylist(PlaylistInsert(title, description, isPublic = isPublic))
                loadMyPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            try {
                repo.deletePlaylist(id)
                loadMyPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updatePlaylist(id: String, update: PlaylistUpdate) {
        viewModelScope.launch {
            try {
                repo.updatePlaylist(id, update)
                loadMyPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// LIKED SONGS VIEWMODEL
// ═══════════════════════════════════════════════════
class LikedSongsViewModel(
    private val repo: LikedSongsRepository = LikedSongsRepository(),
    private val songsRepo: SongsRepository = SongsRepository()
) : ViewModel() {

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadLikedSongs() }

    fun loadLikedSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val liked = repo.getMyLikedSongs()
                val songs = liked.mapNotNull { songsRepo.getSong(it.songId) }
                _likedSongs.value = songs
            } catch (e: Exception) {
                // handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(songId: String) {
        viewModelScope.launch {
            try {
                if (repo.isLiked(songId)) {
                    repo.unlikeSong(songId)
                } else {
                    repo.likeSong(songId)
                }
                loadLikedSongs()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// LISTENING HISTORY VIEWMODEL
// ═══════════════════════════════════════════════════
class HistoryViewModel(
    private val historyRepo: ListeningHistoryRepository = ListeningHistoryRepository(),
    private val recentRepo: RecentlyPlayedRepository = RecentlyPlayedRepository()
) : ViewModel() {

    private val _history = MutableStateFlow<List<ListeningHistory>>(emptyList())
    val history: StateFlow<List<ListeningHistory>> = _history.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<RecentlyPlayed>>(emptyList())
    val recentlyPlayed: StateFlow<List<RecentlyPlayed>> = _recentlyPlayed.asStateFlow()

    init {
        loadHistory()
        loadRecentlyPlayed()
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                _history.value = historyRepo.getHistory()
            } catch (e: Exception) { /* handle */ }
        }
    }

    fun loadRecentlyPlayed() {
        viewModelScope.launch {
            try {
                _recentlyPlayed.value = recentRepo.getRecentlyPlayed()
            } catch (e: Exception) { /* handle */ }
        }
    }

    fun recordPlay(songId: String, durationSeconds: Int) {
        viewModelScope.launch {
            try {
                historyRepo.recordPlay(songId, durationSeconds)
                recentRepo.bumpRecentlyPlayed(songId)
                loadRecentlyPlayed()
            } catch (e: Exception) { /* handle */ }
        }
    }
}

// ═══════════════════════════════════════════════════
// PROFILE VIEWMODEL
// ═══════════════════════════════════════════════════
class ProfileViewModel(
    private val repo: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _profile.value = repo.getMyProfile()
            } catch (e: Exception) { /* handle */ }
            _isLoading.value = false
        }
    }

    fun updateProfile(update: ProfileUpdate) {
        viewModelScope.launch {
            try {
                _profile.value = repo.updateProfile(update)
            } catch (e: Exception) { /* handle */ }
        }
    }
}

// ═══════════════════════════════════════════════════
// SETTINGS VIEWMODEL
// ═══════════════════════════════════════════════════
class SettingsViewModel(
    private val repo: UserSettingsRepository = UserSettingsRepository()
) : ViewModel() {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    init { loadSettings() }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                _settings.value = repo.getMySettings()
            } catch (e: Exception) { /* handle */ }
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            try {
                _settings.value = repo.updateSettings(UserSettingsUpdate(theme = theme))
            } catch (e: Exception) { /* handle */ }
        }
    }

    fun toggleAutoplay(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _settings.value = repo.updateSettings(UserSettingsUpdate(autoplayEnabled = enabled))
            } catch (e: Exception) { /* handle */ }
        }
    }
}

// ═══════════════════════════════════════════════════
// NOTIFICATIONS VIEWMODEL
// ═══════════════════════════════════════════════════
class NotificationsViewModel(
    private val repo: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    init { loadNotifications() }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _notifications.value = repo.getNotifications()
                _unreadCount.value = repo.getUnreadCount()
            } catch (e: Exception) { /* handle */ }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                repo.markAsRead(id)
                loadNotifications()
            } catch (e: Exception) { /* handle */ }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                repo.markAllAsRead()
                loadNotifications()
            } catch (e: Exception) { /* handle */ }
        }
    }
}

// ═══════════════════════════════════════════════════
// SEARCH VIEWMODEL
// ═══════════════════════════════════════════════════
class SearchViewModel(
    private val songsRepo: SongsRepository = SongsRepository()
) : ViewModel() {

    private val _results = MutableStateFlow<List<Song>>(emptyList())
    val results: StateFlow<List<Song>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _results.value = songsRepo.searchSongs(query)
            } catch (e: Exception) { /* handle */ }
            _isLoading.value = false
        }
    }
}
