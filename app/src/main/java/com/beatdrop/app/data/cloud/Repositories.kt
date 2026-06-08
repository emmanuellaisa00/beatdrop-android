package com.beatdrop.app.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Lightweight repository facades kept for cloud ViewModels. The production sync
// path uses CloudSync directly; these methods remain best-effort/no-op so cloud
// optional features never break signed-out/local playback.
class PlaylistRepository {
    suspend fun getMyPlaylists(): List<Playlist> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun getPublicPlaylists(limit: Long = 50): List<Playlist> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun getPlaylist(id: String): Playlist? = withContext(Dispatchers.IO) { null }
    suspend fun createPlaylist(playlist: PlaylistInsert): Playlist = withContext(Dispatchers.IO) {
        Playlist(
            id = "local_${System.currentTimeMillis()}",
            userId = Supabase.currentUserId.orEmpty(),
            title = playlist.title,
            description = playlist.description,
            coverImageUrl = playlist.coverImageUrl,
            isPublic = playlist.isPublic,
            createdAt = "",
            updatedAt = "",
        )
    }
    suspend fun updatePlaylist(id: String, update: PlaylistUpdate): Playlist = withContext(Dispatchers.IO) {
        Playlist(
            id = id,
            userId = Supabase.currentUserId.orEmpty(),
            title = update.title ?: "Playlist",
            description = update.description,
            coverImageUrl = update.coverImageUrl,
            isPublic = update.isPublic ?: false,
            isCollaborative = update.isCollaborative ?: false,
            createdAt = "",
            updatedAt = "",
        )
    }
    suspend fun deletePlaylist(id: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun searchPlaylists(query: String): List<Playlist> = withContext(Dispatchers.IO) { emptyList() }
}

class PlaylistSongsRepository {
    suspend fun getSongsInPlaylist(playlistId: String): List<PlaylistSong> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun addSongToPlaylist(item: PlaylistSongInsert): Unit = withContext(Dispatchers.IO) {}
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun updatePosition(playlistId: String, songId: String, newPosition: Int): Unit = withContext(Dispatchers.IO) {}
}

class LikedSongsRepository {
    suspend fun getMyLikedSongs(): List<LikedSong> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun likeSong(songId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun unlikeSong(songId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun isLiked(songId: String): Boolean = withContext(Dispatchers.IO) { false }
}

class ListeningHistoryRepository {
    suspend fun getHistory(limit: Long = 100): List<ListeningHistory> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun recordPlay(songId: String, playDurationSeconds: Int): Unit = withContext(Dispatchers.IO) {}
}

class RecentlyPlayedRepository {
    suspend fun getRecentlyPlayed(limit: Long = 30): List<RecentlyPlayed> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun bumpRecentlyPlayed(songId: String): Unit = withContext(Dispatchers.IO) {}
}

class ProfileRepository {
    suspend fun getMyProfile(): Profile? = withContext(Dispatchers.IO) { null }
    suspend fun getProfile(userId: String): Profile? = withContext(Dispatchers.IO) { null }
    suspend fun updateProfile(update: ProfileUpdate): Profile = withContext(Dispatchers.IO) {
        Profile(Supabase.currentUserId.orEmpty(), update.username, update.displayName, update.bio, update.avatarUrl, update.country, "", "")
    }
    suspend fun searchProfiles(query: String): List<Profile> = withContext(Dispatchers.IO) { emptyList() }
}

class UserSettingsRepository {
    suspend fun getMySettings(): UserSettings? = withContext(Dispatchers.IO) { null }
    suspend fun updateSettings(update: UserSettingsUpdate): UserSettings = withContext(Dispatchers.IO) {
        UserSettings(
            userId = Supabase.currentUserId.orEmpty(),
            theme = update.theme ?: "system",
            language = update.language ?: "en",
            streamingQuality = update.streamingQuality ?: "high",
            downloadQuality = update.downloadQuality ?: "high",
            autoplayEnabled = update.autoplayEnabled ?: true,
            crossfadeEnabled = update.crossfadeEnabled ?: false,
            normalizeVolumeEnabled = update.normalizeVolumeEnabled ?: true,
            explicitContentEnabled = update.explicitContentEnabled ?: true,
            updatedAt = "",
        )
    }
}

class SongsRepository {
    suspend fun getAllSongs(limit: Long = 100): List<Song> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun getSong(id: String): Song? = withContext(Dispatchers.IO) { null }
    suspend fun getSongsByArtist(artistName: String): List<Song> = withContext(Dispatchers.IO) { emptyList() }
}

class FollowedArtistsRepository {
    suspend fun getFollowedArtists(): List<FollowedArtist> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun followArtist(artistId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun unfollowArtist(artistId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun isFollowing(artistId: String): Boolean = withContext(Dispatchers.IO) { false }
}

class NotificationsRepository {
    suspend fun getNotifications(): List<Notification> = withContext(Dispatchers.IO) { emptyList() }
    suspend fun getUnreadCount(): Long = withContext(Dispatchers.IO) { 0L }
    suspend fun markAsRead(notificationId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun markAllAsRead(): Unit = withContext(Dispatchers.IO) {}
    suspend fun deleteNotification(id: String): Unit = withContext(Dispatchers.IO) {}
}
