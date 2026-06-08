package com.beatdrop.app.data.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ──────────────────────────────────────────
// PROFILES
// ──────────────────────────────────────────
@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val country: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ProfileUpdate(
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val country: String? = null
)

// ──────────────────────────────────────────
// SONGS (public catalog)
// ──────────────────────────────────────────
@Serializable
data class Song(
    val id: String,
    val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("album_name") val albumName: String? = null,
    val duration: Int = 0,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("external_source_id") val externalSourceId: String? = null,
    @SerialName("metadata_json") val metadataJson: JsonElement? = null,
    @SerialName("created_at") val createdAt: String
)

// ──────────────────────────────────────────
// PLAYLISTS
// ──────────────────────────────────────────
@Serializable
data class Playlist(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class PlaylistInsert(
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false
)

@Serializable
data class PlaylistUpdate(
    val title: String? = null,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null
)

// ──────────────────────────────────────────
// PLAYLIST SONGS (junction)
// ──────────────────────────────────────────
@Serializable
data class PlaylistSong(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String,
    val position: Int = 0,
    @SerialName("added_at") val addedAt: String
)

@Serializable
data class PlaylistSongInsert(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String,
    val position: Int = 0
)

// ──────────────────────────────────────────
// LIKED SONGS
// ──────────────────────────────────────────
@Serializable
data class LikedSong(
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("liked_at") val likedAt: String
)

// ──────────────────────────────────────────
// LISTENING HISTORY
// ──────────────────────────────────────────
@Serializable
data class ListeningHistory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("played_at") val playedAt: String,
    @SerialName("play_duration") val playDuration: Int = 0
)

@Serializable
data class ListeningHistoryInsert(
    @SerialName("song_id") val songId: String,
    @SerialName("play_duration") val playDuration: Int = 0
)

// ──────────────────────────────────────────
// RECENTLY PLAYED
// ──────────────────────────────────────────
@Serializable
data class RecentlyPlayed(
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("played_at") val playedAt: String
)

// ──────────────────────────────────────────
// USER SETTINGS
// ──────────────────────────────────────────
@Serializable
data class UserSettings(
    @SerialName("user_id") val userId: String,
    val theme: String = "system",
    val language: String = "en",
    @SerialName("streaming_quality") val streamingQuality: String = "high",
    @SerialName("download_quality") val downloadQuality: String = "high",
    @SerialName("autoplay_enabled") val autoplayEnabled: Boolean = true,
    @SerialName("crossfade_enabled") val crossfadeEnabled: Boolean = false,
    @SerialName("normalize_volume_enabled") val normalizeVolumeEnabled: Boolean = true,
    @SerialName("explicit_content_enabled") val explicitContentEnabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class UserSettingsUpdate(
    val theme: String? = null,
    val language: String? = null,
    @SerialName("streaming_quality") val streamingQuality: String? = null,
    @SerialName("download_quality") val downloadQuality: String? = null,
    @SerialName("autoplay_enabled") val autoplayEnabled: Boolean? = null,
    @SerialName("crossfade_enabled") val crossfadeEnabled: Boolean? = null,
    @SerialName("normalize_volume_enabled") val normalizeVolumeEnabled: Boolean? = null,
    @SerialName("explicit_content_enabled") val explicitContentEnabled: Boolean? = null
)

// ──────────────────────────────────────────
// FOLLOWED ARTISTS
// ──────────────────────────────────────────
@Serializable
data class FollowedArtist(
    @SerialName("user_id") val userId: String,
    @SerialName("artist_id") val artistId: String,
    @SerialName("followed_at") val followedAt: String
)

// ──────────────────────────────────────────
// NOTIFICATIONS
// ──────────────────────────────────────────
@Serializable
data class Notification(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String? = null,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class NotificationUpdate(
    val read: Boolean? = null
)
