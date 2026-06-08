package com.beatdrop.app.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Storage is vendored for future avatar/cover wiring. Keep it best-effort and
// compile-stable until the Photo Picker/upload feature is implemented.
class StorageManager {
    companion object {
        const val BUCKET_PROFILE_IMAGES = "profile-images"
        const val BUCKET_PLAYLIST_COVERS = "playlist-covers"
        const val BUCKET_ARTIST_IMAGES = "artist-images"
        const val BUCKET_APP_ASSETS = "app-assets"
    }

    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) { "$userId/avatar.jpg" }
    suspend fun getAvatarUrl(userId: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) { null }
    suspend fun uploadPlaylistCover(userId: String, playlistId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) { "$userId/$playlistId.jpg" }
    suspend fun getPlaylistCoverUrl(playlistId: String, userId: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) { null }
    suspend fun getArtistImageUrl(artistId: String, fileName: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) { null }
    suspend fun getAppAssetUrl(path: String, expiresInSeconds: Int = 3600): String? = withContext(Dispatchers.IO) { null }
    suspend fun deleteAvatar(userId: String): Unit = withContext(Dispatchers.IO) {}
    suspend fun deletePlaylistCover(userId: String, playlistId: String): Unit = withContext(Dispatchers.IO) {}
}
