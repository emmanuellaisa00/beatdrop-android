package com.beatdrop.app.data.cloud

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

// ═══════════════════════════════════════════════════
// REALTIME NOTIFICATIONS MANAGER
// ═══════════════════════════════════════════════════
class NotificationsRealtimeManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private val _notifications = MutableSharedFlow<Notification>(replay = 0)
    val notifications: SharedFlow<Notification> = _notifications.asSharedFlow()

    fun subscribe(userId: String) {
        scope.launch {
            val ch = Supabase.realtime.channel("notifications-$userId")
            channel = ch

            ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }.onEach { action ->
                val notification = action.decodeRecord<Notification>()
                _notifications.emit(notification)
            }.launchIn(scope)

            ch.subscribe()
        }
    }

    fun unsubscribe() {
        scope.launch {
            channel?.unsubscribe()
            channel = null
        }
    }
}

// ═══════════════════════════════════════════════════
// REALTIME PLAYLISTS MANAGER
// ═══════════════════════════════════════════════════
class PlaylistsRealtimeManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private val _playlistChanges = MutableSharedFlow<PlaylistChange>(replay = 0)
    val playlistChanges: SharedFlow<PlaylistChange> = _playlistChanges.asSharedFlow()

    sealed class PlaylistChange {
        data class Inserted(val playlist: Playlist) : PlaylistChange()
        data class Updated(val playlist: Playlist) : PlaylistChange()
        data class Deleted(val id: String) : PlaylistChange()
    }

    fun subscribe(userId: String) {
        scope.launch {
            val ch = Supabase.realtime.channel("playlists-$userId")
            channel = ch

            // Listen for inserts
            ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "playlists"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }.onEach { action ->
                _playlistChanges.emit(PlaylistChange.Inserted(action.decodeRecord()))
            }.launchIn(scope)

            // Listen for updates
            ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "playlists"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }.onEach { action ->
                _playlistChanges.emit(PlaylistChange.Updated(action.decodeRecord()))
            }.launchIn(scope)

            // Listen for deletes
            ch.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "playlists"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }.onEach { action ->
                val id = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull ?: return@onEach
                _playlistChanges.emit(PlaylistChange.Deleted(id))
            }.launchIn(scope)

            ch.subscribe()
        }
    }

    fun unsubscribe() {
        scope.launch {
            channel?.unsubscribe()
            channel = null
        }
    }
}

// ═══════════════════════════════════════════════════
// REALTIME LIKED SONGS MANAGER
// ═══════════════════════════════════════════════════
class LikedSongsRealtimeManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private val _likedSongChanges = MutableSharedFlow<LikedSongChange>(replay = 0)
    val likedSongChanges: SharedFlow<LikedSongChange> = _likedSongChanges.asSharedFlow()

    sealed class LikedSongChange {
        data class Liked(val songId: String) : LikedSongChange()
        data class Unliked(val songId: String) : LikedSongChange()
    }

    fun subscribe(userId: String) {
        scope.launch {
            val ch = Supabase.realtime.channel("liked-songs-$userId")
            channel = ch

            ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "liked_songs"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }.onEach { action ->
                val likedSong = action.decodeRecord<LikedSong>()
                _likedSongChanges.emit(LikedSongChange.Liked(likedSong.songId))
            }.launchIn(scope)

            ch.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "liked_songs"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }.onEach { action ->
                val songId = action.oldRecord["song_id"]?.jsonPrimitive?.contentOrNull ?: return@onEach
                _likedSongChanges.emit(LikedSongChange.Unliked(songId))
            }.launchIn(scope)

            ch.subscribe()
        }
    }

    fun unsubscribe() {
        scope.launch {
            channel?.unsubscribe()
            channel = null
        }
    }
}

// ═══════════════════════════════════════════════════
// REALTIME PLAYLIST SONGS MANAGER
// ═══════════════════════════════════════════════════
class PlaylistSongsRealtimeManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private val _changes = MutableSharedFlow<PlaylistSongChange>(replay = 0)
    val changes: SharedFlow<PlaylistSongChange> = _changes.asSharedFlow()

    sealed class PlaylistSongChange {
        data class Added(val item: PlaylistSong) : PlaylistSongChange()
        data class Removed(val playlistId: String, val songId: String) : PlaylistSongChange()
        data class Reordered(val playlistId: String, val songId: String, val newPosition: Int) : PlaylistSongChange()
    }

    fun subscribe(playlistId: String) {
        scope.launch {
            val ch = Supabase.realtime.channel("playlist-songs-$playlistId")
            channel = ch

            ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "playlist_songs"
                filter("playlist_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, playlistId)
            }.onEach { action ->
                _changes.emit(PlaylistSongChange.Added(action.decodeRecord()))
            }.launchIn(scope)

            ch.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "playlist_songs"
                filter("playlist_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, playlistId)
            }.onEach { action ->
                val pid = action.oldRecord["playlist_id"]?.jsonPrimitive?.contentOrNull ?: return@onEach
                val sid = action.oldRecord["song_id"]?.jsonPrimitive?.contentOrNull ?: return@onEach
                _changes.emit(PlaylistSongChange.Removed(pid, sid))
            }.launchIn(scope)

            ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "playlist_songs"
                filter("playlist_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, playlistId)
            }.onEach { action ->
                val pid = action.record["playlist_id"]?.jsonPrimitive?.contentOrNull ?: return@onEach
                val sid = action.record["song_id"]?.jsonPrimitive?.contentOrNull ?: return@onEach
                val pos = action.record["position"]?.jsonPrimitive?.intOrNull ?: return@onEach
                _changes.emit(PlaylistSongChange.Reordered(pid, sid, pos))
            }.launchIn(scope)

            ch.subscribe()
        }
    }

    fun unsubscribe() {
        scope.launch {
            channel?.unsubscribe()
            channel = null
        }
    }
}
