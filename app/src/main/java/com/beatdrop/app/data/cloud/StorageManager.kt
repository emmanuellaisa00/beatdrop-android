package com.beatdrop.app.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════
// STORAGE MANAGER
// ═══════════════════════════════════════════════════
class StorageManager {

    companion object {
        const val BUCKET_PROFILE_IMAGES = "profile-images"
        const val BUCKET_PLAYLIST_COVERS = "playlist-covers"
        const val BUCKET_ARTIST_IMAGES = "artist-images"
        const val BUCKET_APP_ASSETS = "app-assets"
    }

    // ─── PROFILE AVATAR ────────────────────────────────
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val path = "$userId/avatar.jpg"
        Supabase.storage.from(BUCKET_PROFILE_IMAGES).upload(path, imageBytes) { upsert = true }
        path
    }

    suspend fun getAvatarUrl(userId: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) {
        try {
            Supabase.storage.from(BUCKET_PROFILE_IMAGES).createSignedUrl(
                "$userId/avatar.jpg",
                expiresInSeconds
            )
        } catch (e: Exception) {
            null
        }
    }

    // ─── PLAYLIST COVER ────────────────────────────────
    suspend fun uploadPlaylistCover(userId: String, playlistId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val path = "$userId/$playlistId.jpg"
        Supabase.storage.from(BUCKET_PLAYLIST_COVERS).upload(path, imageBytes) { upsert = true }
        path
    }

    suspend fun getPlaylistCoverUrl(playlistId: String, userId: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) {
        try {
            Supabase.storage.from(BUCKET_PLAYLIST_COVERS).createSignedUrl(
                "$userId/$playlistId.jpg",
                expiresInSeconds
            )
        } catch (e: Exception) {
            null
        }
    }

    // ─── ARTIST IMAGES (read-only for app) ─────────────
    suspend fun getArtistImageUrl(artistId: String, fileName: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) {
        try {
            Supabase.storage.from(BUCKET_ARTIST_IMAGES).createSignedUrl(
                "$artistId/$fileName",
                expiresInSeconds
            )
        } catch (e: Exception) {
            null
        }
    }

    // ─── APP ASSETS (read-only for app) ────────────────
    suspend fun getAppAssetUrl(path: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) {
        try {
            Supabase.storage.from(BUCKET_APP_ASSETS).createSignedUrl(path, expiresInSeconds)
        } catch (e: Exception) {
            null
        }
    }

    // ─── DELETE ────────────────────────────────────────
    suspend fun deleteAvatar(userId: String): Unit = withContext(Dispatchers.IO) {
        Supabase.storage.from(BUCKET_PROFILE_IMAGES).delete("$userId/avatar.jpg")
    }

    suspend fun deletePlaylistCover(userId: String, playlistId: String): Unit = withContext(Dispatchers.IO) {
        Supabase.storage.from(BUCKET_PLAYLIST_COVERS).delete("$userId/$playlistId.jpg")
    }
}
