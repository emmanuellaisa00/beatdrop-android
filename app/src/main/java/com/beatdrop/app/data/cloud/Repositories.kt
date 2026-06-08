package com.beatdrop.app.data.cloud

import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ═══════════════════════════════════════════════════
// PLAYLIST REPOSITORY
// ═══════════════════════════════════════════════════
class PlaylistRepository {

    private val table get() = Supabase.postgrest.from("playlists")

    suspend fun getMyPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("user_id", Supabase.currentUserId!!) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Playlist>()
    }

    suspend fun getPublicPlaylists(limit: Long = 50): List<Playlist> = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("is_public", true) }
            order("created_at", Order.DESCENDING)
            limit(count = limit)
        }.decodeList<Playlist>()
    }

    suspend fun getPlaylist(id: String): Playlist? = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("id", id) }
            limit(count = 1)
        }.decodeSingleOrNull<Playlist>()
    }

    suspend fun createPlaylist(playlist: PlaylistInsert): Playlist = withContext(Dispatchers.IO) {
        table.insert(playlist) { select() }.decodeSingle<Playlist>()
    }

    suspend fun updatePlaylist(id: String, update: PlaylistUpdate): Playlist = withContext(Dispatchers.IO) {
        table.update(update) {
            filter { eq("id", id) }
            select()
        }.decodeSingle<Playlist>()
    }

    suspend fun deletePlaylist(id: String): Unit = withContext(Dispatchers.IO) {
        table.delete { filter { eq("id", id) } }
    }

    suspend fun searchPlaylists(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        table.select {
            filter { textSearch("title", query, TextSearchType.PLAINTO) }
            limit(count = 20)
        }.decodeList<Playlist>()
    }
}

// ═══════════════════════════════════════════════════
// PLAYLIST SONGS REPOSITORY
// ═══════════════════════════════════════════════════
class PlaylistSongsRepository {

    private val table get() = Supabase.postgrest.from("playlist_songs")

    suspend fun getSongsInPlaylist(playlistId: String): List<PlaylistSong> = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("playlist_id", playlistId) }
            order("position", Order.ASCENDING)
        }.decodeList<PlaylistSong>()
    }

    suspend fun addSongToPlaylist(item: PlaylistSongInsert): Unit = withContext(Dispatchers.IO) {
        table.insert(item)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Unit = withContext(Dispatchers.IO) {
        table.delete {
            filter {
                eq("playlist_id", playlistId)
                eq("song_id", songId)
            }
        }
    }

    suspend fun updatePosition(playlistId: String, songId: String, newPosition: Int): Unit = withContext(Dispatchers.IO) {
        table.update(buildJsonObject { put("position", newPosition) }) {
            filter {
                eq("playlist_id", playlistId)
                eq("song_id", songId)
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// LIKED SONGS REPOSITORY
// ═══════════════════════════════════════════════════
class LikedSongsRepository {

    private val table get() = Supabase.postgrest.from("liked_songs")

    suspend fun getMyLikedSongs(): List<LikedSong> = withContext(Dispatchers.IO) {
        table.select {
            order("liked_at", Order.DESCENDING)
        }.decodeList<LikedSong>()
    }

    suspend fun likeSong(songId: String): Unit = withContext(Dispatchers.IO) {
        table.insert(mapOf("song_id" to songId))
    }

    suspend fun unlikeSong(songId: String): Unit = withContext(Dispatchers.IO) {
        table.delete { filter { eq("song_id", songId) } }
    }

    suspend fun isLiked(songId: String): Boolean = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("song_id", songId) }
            limit(count = 1)
        }.decodeList<LikedSong>().isNotEmpty()
    }
}

// ═══════════════════════════════════════════════════
// LISTENING HISTORY REPOSITORY
// ═══════════════════════════════════════════════════
class ListeningHistoryRepository {

    private val table get() = Supabase.postgrest.from("listening_history")

    suspend fun getHistory(limit: Long = 100): List<ListeningHistory> = withContext(Dispatchers.IO) {
        table.select {
            order("played_at", Order.DESCENDING)
            limit(count = limit)
        }.decodeList<ListeningHistory>()
    }

    suspend fun recordPlay(songId: String, playDurationSeconds: Int): Unit = withContext(Dispatchers.IO) {
        table.insert(ListeningHistoryInsert(songId, playDurationSeconds))
    }
}

// ═══════════════════════════════════════════════════
// RECENTLY PLAYED REPOSITORY
// ═══════════════════════════════════════════════════
class RecentlyPlayedRepository {

    private val table get() = Supabase.postgrest.from("recently_played")

    suspend fun getRecentlyPlayed(limit: Long = 30): List<RecentlyPlayed> = withContext(Dispatchers.IO) {
        table.select {
            order("played_at", Order.DESCENDING)
            limit(count = limit)
        }.decodeList<RecentlyPlayed>()
    }

    suspend fun bumpRecentlyPlayed(songId: String): Unit = withContext(Dispatchers.IO) {
        table.upsert(
            buildJsonObject { put("song_id", songId) },
            onConflict = "user_id,song_id"
        )
    }
}

// ═══════════════════════════════════════════════════
// PROFILE REPOSITORY
// ═══════════════════════════════════════════════════
class ProfileRepository {

    private val table get() = Supabase.postgrest.from("profiles")

    suspend fun getMyProfile(): Profile? = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("id", Supabase.currentUserId!!) }
            limit(count = 1)
        }.decodeSingleOrNull<Profile>()
    }

    suspend fun getProfile(userId: String): Profile? = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("id", userId) }
            limit(count = 1)
        }.decodeSingleOrNull<Profile>()
    }

    suspend fun updateProfile(update: ProfileUpdate): Profile = withContext(Dispatchers.IO) {
        table.update(update) {
            filter { eq("id", Supabase.currentUserId!!) }
            select()
        }.decodeSingle<Profile>()
    }

    suspend fun searchProfiles(query: String): List<Profile> = withContext(Dispatchers.IO) {
        table.select {
            filter { textSearch("username", query, TextSearchType.PLAINTO) }
            limit(count = 20)
        }.decodeList<Profile>()
    }
}

// ═══════════════════════════════════════════════════
// USER SETTINGS REPOSITORY
// ═══════════════════════════════════════════════════
class UserSettingsRepository {

    private val table get() = Supabase.postgrest.from("user_settings")

    suspend fun getMySettings(): UserSettings? = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("user_id", Supabase.currentUserId!!) }
            limit(count = 1)
        }.decodeSingleOrNull<UserSettings>()
    }

    suspend fun updateSettings(update: UserSettingsUpdate): UserSettings = withContext(Dispatchers.IO) {
        table.update(update) {
            filter { eq("user_id", Supabase.currentUserId!!) }
            select()
        }.decodeSingle<UserSettings>()
    }
}

// ═══════════════════════════════════════════════════
// SONGS REPOSITORY (public catalog)
// ═══════════════════════════════════════════════════
class SongsRepository {

    private val table get() = Supabase.postgrest.from("songs")

    suspend fun getAllSongs(limit: Long = 100): List<Song> = withContext(Dispatchers.IO) {
        table.select { limit(count = limit) }.decodeList<Song>()
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        table.select {
            filter { textSearch("title", query, TextSearchType.PLAINTO) }
            limit(count = 30)
        }.decodeList<Song>()
    }

    suspend fun getSong(id: String): Song? = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("id", id) }
            limit(count = 1)
        }.decodeSingleOrNull<Song>()
    }

    suspend fun getSongsByArtist(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("artist_name", artistName) }
            order("title", Order.ASCENDING)
        }.decodeList<Song>()
    }
}

// ═══════════════════════════════════════════════════
// FOLLOWED ARTISTS REPOSITORY
// ═══════════════════════════════════════════════════
class FollowedArtistsRepository {

    private val table get() = Supabase.postgrest.from("followed_artists")

    suspend fun getFollowedArtists(): List<FollowedArtist> = withContext(Dispatchers.IO) {
        table.select { order("followed_at", Order.DESCENDING) }.decodeList<FollowedArtist>()
    }

    suspend fun followArtist(artistId: String): Unit = withContext(Dispatchers.IO) {
        table.insert(mapOf("artist_id" to artistId))
    }

    suspend fun unfollowArtist(artistId: String): Unit = withContext(Dispatchers.IO) {
        table.delete { filter { eq("artist_id", artistId) } }
    }

    suspend fun isFollowing(artistId: String): Boolean = withContext(Dispatchers.IO) {
        table.select {
            filter { eq("artist_id", artistId) }
            limit(count = 1)
        }.decodeList<FollowedArtist>().isNotEmpty()
    }
}

// ═══════════════════════════════════════════════════
// NOTIFICATIONS REPOSITORY
// ═══════════════════════════════════════════════════
class NotificationsRepository {

    private val table get() = Supabase.postgrest.from("notifications")

    suspend fun getNotifications(): List<Notification> = withContext(Dispatchers.IO) {
        table.select {
            order("created_at", Order.DESCENDING)
        }.decodeList<Notification>()
    }

    suspend fun getUnreadCount(): Long = withContext(Dispatchers.IO) {
        // Simple + portable across SDK versions: fetch unread rows and count them.
        table.select {
            filter { eq("read", false) }
        }.decodeList<Notification>().size.toLong()
    }

    suspend fun markAsRead(notificationId: String): Unit = withContext(Dispatchers.IO) {
        table.update(buildJsonObject { put("read", true) }) { filter { eq("id", notificationId) } }
    }

    suspend fun markAllAsRead(): Unit = withContext(Dispatchers.IO) {
        table.update(buildJsonObject { put("read", true) }) { filter { eq("read", false) } }
    }

    suspend fun deleteNotification(id: String): Unit = withContext(Dispatchers.IO) {
        table.delete { filter { eq("id", id) } }
    }
}
