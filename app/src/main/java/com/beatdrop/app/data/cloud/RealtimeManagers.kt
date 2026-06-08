package com.beatdrop.app.data.cloud

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Realtime is optional; no-op managers keep the app local-first if sockets fail or are disabled. */
class NotificationsRealtimeManager {
    private val _notifications = MutableSharedFlow<Notification>(replay = 0)
    val notifications: SharedFlow<Notification> = _notifications.asSharedFlow()
    fun subscribe(userId: String) = Unit
    fun unsubscribe() = Unit
}

class PlaylistsRealtimeManager {
    private val _playlistChanges = MutableSharedFlow<PlaylistChange>(replay = 0)
    val playlistChanges: SharedFlow<PlaylistChange> = _playlistChanges.asSharedFlow()

    sealed class PlaylistChange {
        data class Inserted(val playlist: Playlist) : PlaylistChange()
        data class Updated(val playlist: Playlist) : PlaylistChange()
        data class Deleted(val id: String) : PlaylistChange()
    }

    fun subscribe(userId: String) = Unit
    fun unsubscribe() = Unit
}

class LikedSongsRealtimeManager {
    private val _likedSongChanges = MutableSharedFlow<LikedSongChange>(replay = 0)
    val likedSongChanges: SharedFlow<LikedSongChange> = _likedSongChanges.asSharedFlow()

    sealed class LikedSongChange {
        data class Liked(val songId: String) : LikedSongChange()
        data class Unliked(val songId: String) : LikedSongChange()
    }

    fun subscribe(userId: String) = Unit
    fun unsubscribe() = Unit
}

class PlaylistSongsRealtimeManager {
    private val _changes = MutableSharedFlow<PlaylistSongChange>(replay = 0)
    val changes: SharedFlow<PlaylistSongChange> = _changes.asSharedFlow()

    sealed class PlaylistSongChange {
        data class Added(val item: PlaylistSong) : PlaylistSongChange()
        data class Removed(val playlistId: String, val songId: String) : PlaylistSongChange()
        data class Reordered(val playlistId: String, val songId: String, val newPosition: Int) : PlaylistSongChange()
    }

    fun subscribe(playlistId: String) = Unit
    fun unsubscribe() = Unit
}
