package com.beatdrop.app.data.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ──────────────────────────────────────────
// AUTH / PROFILES
// ──────────────────────────────────────────
@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val country: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
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
// EXTERNAL ITEMS CACHE (YouTube/Innertube metadata cache, NOT full catalogue)
// ──────────────────────────────────────────
@Serializable
data class ExternalItem(
    val id: String,
    @SerialName("external_source") val externalSource: String = "youtube",
    @SerialName("external_type") val externalType: String,
    @SerialName("external_id") val externalId: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("album_name") val albumName: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val year: String? = null,
    @SerialName("metadata_json") val metadataJson: JsonElement? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ExternalItemInsert(
    @SerialName("external_source") val externalSource: String = "youtube",
    @SerialName("external_type") val externalType: String,
    @SerialName("external_id") val externalId: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("album_name") val albumName: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val year: String? = null,
    @SerialName("metadata_json") val metadataJson: JsonElement? = null,
)

// Backwards-compatible aliases for older ViewModels. These now represent cached
// external songs, not an owned catalogue table.
typealias Song = ExternalItem

// ──────────────────────────────────────────
// LIBRARY / LIKES / FOLLOWS
// ──────────────────────────────────────────
@Serializable
data class LibraryItem(
    @SerialName("user_id") val userId: String,
    @SerialName("external_item_id") val externalItemId: String,
    @SerialName("item_type") val itemType: String,
    @SerialName("saved_at") val savedAt: String = ""
)

@Serializable
data class LikedItem(
    @SerialName("user_id") val userId: String,
    @SerialName("external_item_id") val externalItemId: String,
    @SerialName("liked_at") val likedAt: String = ""
)

typealias LikedSong = LikedItem

// Compatibility for older cloud ViewModels that still say songId.
val LikedItem.songId: String get() = externalItemId
val PlaylistItem.songId: String get() = externalItemId

@Serializable
data class FollowedArtist(
    @SerialName("user_id") val userId: String,
    @SerialName("artist_external_id") val artistExternalId: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("followed_at") val followedAt: String = ""
)

@Serializable
data class UserFavoriteArtist(
    @SerialName("user_id") val userId: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("artist_external_id") val artistExternalId: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    val source: String = "onboarding",
    @SerialName("created_at") val createdAt: String = ""
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
    @SerialName("is_collaborative") val isCollaborative: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class PlaylistInsert(
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("is_collaborative") val isCollaborative: Boolean = false
)

@Serializable
data class PlaylistUpdate(
    val title: String? = null,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("is_collaborative") val isCollaborative: Boolean? = null
)

@Serializable
data class PlaylistItem(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("external_item_id") val externalItemId: String,
    val position: Int = 0,
    @SerialName("added_by") val addedBy: String? = null,
    @SerialName("added_at") val addedAt: String = ""
)

@Serializable
data class PlaylistItemInsert(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("external_item_id") val externalItemId: String,
    val position: Int = 0,
    @SerialName("added_by") val addedBy: String? = null,
)

typealias PlaylistSong = PlaylistItem
typealias PlaylistSongInsert = PlaylistItemInsert

// ──────────────────────────────────────────
// HISTORY / RECENTS / SEARCH
// ──────────────────────────────────────────
@Serializable
data class ListeningHistory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("external_item_id") val externalItemId: String,
    @SerialName("played_at") val playedAt: String = "",
    @SerialName("play_duration_seconds") val playDurationSeconds: Int = 0,
    val completed: Boolean = false,
    @SerialName("source_context") val sourceContext: String? = null,
    @SerialName("source_context_id") val sourceContextId: String? = null
)

@Serializable
data class ListeningHistoryInsert(
    @SerialName("external_item_id") val externalItemId: String,
    @SerialName("play_duration_seconds") val playDurationSeconds: Int = 0,
    val completed: Boolean = false,
    @SerialName("source_context") val sourceContext: String? = null,
    @SerialName("source_context_id") val sourceContextId: String? = null
)

@Serializable
data class RecentlyPlayed(
    @SerialName("user_id") val userId: String,
    @SerialName("external_item_id") val externalItemId: String,
    @SerialName("played_at") val playedAt: String = "",
    @SerialName("play_count") val playCount: Int = 1
)

@Serializable
data class SearchHistory(
    val id: String,
    @SerialName("user_id") val userId: String,
    val query: String,
    val category: String? = null,
    @SerialName("searched_at") val searchedAt: String = ""
)

// ──────────────────────────────────────────
// USER SETTINGS
// ──────────────────────────────────────────
@Serializable
data class UserSettings(
    @SerialName("user_id") val userId: String,
    val theme: String = "dark",
    val language: String = "en",
    @SerialName("streaming_quality") val streamingQuality: String = "high",
    @SerialName("download_quality") val downloadQuality: String = "high",
    @SerialName("autoplay_enabled") val autoplayEnabled: Boolean = true,
    @SerialName("crossfade_seconds") val crossfadeSeconds: Int = 0,
    @SerialName("crossfade_enabled") val crossfadeEnabled: Boolean = false,
    @SerialName("normalize_volume_enabled") val normalizeVolumeEnabled: Boolean = true,
    @SerialName("explicit_content_enabled") val explicitContentEnabled: Boolean = true,
    @SerialName("show_local_files") val showLocalFiles: Boolean = true,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class UserSettingsUpdate(
    val theme: String? = null,
    val language: String? = null,
    @SerialName("streaming_quality") val streamingQuality: String? = null,
    @SerialName("download_quality") val downloadQuality: String? = null,
    @SerialName("autoplay_enabled") val autoplayEnabled: Boolean? = null,
    @SerialName("crossfade_seconds") val crossfadeSeconds: Int? = null,
    @SerialName("crossfade_enabled") val crossfadeEnabled: Boolean? = null,
    @SerialName("normalize_volume_enabled") val normalizeVolumeEnabled: Boolean? = null,
    @SerialName("explicit_content_enabled") val explicitContentEnabled: Boolean? = null,
    @SerialName("show_local_files") val showLocalFiles: Boolean? = null
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
    val type: String? = null,
    @SerialName("deep_link") val deepLink: String? = null,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class NotificationUpdate(
    val read: Boolean? = null
)
