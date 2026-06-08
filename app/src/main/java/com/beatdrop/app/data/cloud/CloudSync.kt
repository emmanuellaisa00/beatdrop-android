package com.beatdrop.app.data.cloud

import com.beatdrop.app.data.local.UserPlaylist
import com.beatdrop.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Local-first, best-effort Supabase sync bridge. */
object CloudSync {

    val isSignedIn: Boolean get() = Supabase.isAuthenticated()

    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()
    private val songIdCache = ConcurrentHashMap<String, String>()
    private val playlistIdCache = ConcurrentHashMap<String, String>()

    data class RemotePlaylist(
        val cloudId: String,
        val title: String,
        val trackIds: List<String>,
    )

    suspend fun ensureSongId(track: Track): String? = withContext(Dispatchers.IO) {
        songIdCache[track.id]?.let { return@withContext it }
        runCatching {
            val existing = getArray("songs", "select=id,external_source_id&external_source_id=eq.${enc(track.id)}&limit=1")
                .firstObject()
                ?.optString("id")
                ?.takeIf { it.isNotBlank() }
            if (existing != null) {
                songIdCache[track.id] = existing
                return@runCatching existing
            }

            val body = JSONObject().apply {
                put("title", track.title)
                put("artist_name", track.artist)
                if (track.album.isNotBlank()) put("album_name", track.album)
                put("duration", (track.durationMs / 1000).toInt())
                track.artworkUri?.let { put("artwork_url", it.toString()) }
                put("external_source_id", track.id)
            }
            val inserted = requestArray(
                method = "POST",
                table = "songs",
                query = "select=id",
                body = body,
                prefer = "return=representation"
            ).firstObject()?.optString("id")?.takeIf { it.isNotBlank() }
            if (inserted != null) songIdCache[track.id] = inserted
            inserted
        }.getOrNull()
    }

    suspend fun pushLike(track: Track, liked: Boolean) {
        if (!isSignedIn) return
        runCatching {
            val songId = ensureSongId(track) ?: return
            if (liked) {
                requestArray("POST", "liked_songs", body = JSONObject().put("song_id", songId))
            } else {
                requestArray("DELETE", "liked_songs", "song_id=eq.${enc(songId)}")
            }
        }
    }

    suspend fun pullLikedExternalIds(): Set<String> = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext emptySet()
        runCatching {
            getArray("liked_songs", "select=song_id,songs(external_source_id)&order=liked_at.desc")
                .objects()
                .mapNotNull { row -> row.optJSONObject("songs")?.optString("external_source_id")?.takeIf { it.isNotBlank() } }
                .toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun ensurePlaylistId(
        playlist: UserPlaylist,
        onCloudId: (localId: String, cloudId: String) -> Unit = { _, _ -> },
    ): String? = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext null
        playlist.cloudId?.let {
            playlistIdCache[playlist.id] = it
            return@withContext it
        }
        playlistIdCache[playlist.id]?.let { return@withContext it }
        runCatching {
            val body = JSONObject().apply {
                put("title", playlist.name)
                put("is_public", false)
            }
            val cloudId = requestArray("POST", "playlists", "select=id", body, "return=representation")
                .firstObject()
                ?.optString("id")
                ?.takeIf { it.isNotBlank() }
            if (cloudId != null) {
                playlistIdCache[playlist.id] = cloudId
                onCloudId(playlist.id, cloudId)
            }
            cloudId
        }.getOrNull()
    }

    suspend fun pushPlaylistMetadata(
        playlist: UserPlaylist,
        onCloudId: (localId: String, cloudId: String) -> Unit = { _, _ -> },
    ) {
        if (!isSignedIn) return
        runCatching {
            val cloudId = ensurePlaylistId(playlist, onCloudId) ?: return
            requestArray("PATCH", "playlists", "id=eq.${enc(cloudId)}", JSONObject().put("title", playlist.name))
        }
    }

    suspend fun pushPlaylistTrackAdded(
        playlist: UserPlaylist,
        track: Track,
        onCloudId: (localId: String, cloudId: String) -> Unit = { _, _ -> },
    ) {
        if (!isSignedIn) return
        runCatching {
            val cloudPlaylistId = ensurePlaylistId(playlist, onCloudId) ?: return
            val songId = ensureSongId(track) ?: return
            val position = playlist.trackIds.indexOf(track.id).takeIf { it >= 0 } ?: playlist.trackIds.lastIndex.coerceAtLeast(0)
            requestArray("POST", "playlist_songs", body = JSONObject().apply {
                put("playlist_id", cloudPlaylistId)
                put("song_id", songId)
                put("position", position)
            })
        }
    }

    suspend fun pushPlaylistSnapshot(
        playlist: UserPlaylist,
        tracksById: Map<String, Track>,
        onCloudId: (localId: String, cloudId: String) -> Unit = { _, _ -> },
    ) {
        if (!isSignedIn) return
        runCatching {
            val cloudPlaylistId = ensurePlaylistId(playlist, onCloudId) ?: return
            requestArray("PATCH", "playlists", "id=eq.${enc(cloudPlaylistId)}", JSONObject().put("title", playlist.name))
            requestArray("DELETE", "playlist_songs", "playlist_id=eq.${enc(cloudPlaylistId)}")
            playlist.trackIds.forEachIndexed { index, trackId ->
                val track = tracksById[trackId] ?: return@forEachIndexed
                val songId = ensureSongId(track) ?: return@forEachIndexed
                requestArray("POST", "playlist_songs", body = JSONObject().apply {
                    put("playlist_id", cloudPlaylistId)
                    put("song_id", songId)
                    put("position", index)
                })
            }
        }
    }

    suspend fun deleteCloudPlaylist(cloudId: String?) {
        if (!isSignedIn || cloudId == null) return
        runCatching { requestArray("DELETE", "playlists", "id=eq.${enc(cloudId)}") }
    }

    suspend fun pullPlaylists(): List<RemotePlaylist> = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext emptyList()
        runCatching {
            getArray(
                "playlists",
                "select=id,title,created_at&user_id=eq.${enc(Supabase.currentUserId.orEmpty())}&order=created_at.desc"
            ).objects().mapNotNull { row ->
                val id = row.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = row.optString("title", "Cloud Playlist")
                val trackIds = getArray(
                    "playlist_songs",
                    "select=position,songs(external_source_id)&playlist_id=eq.${enc(id)}&order=position.asc"
                ).objects().mapNotNull { item ->
                    item.optJSONObject("songs")?.optString("external_source_id")?.takeIf { it.isNotBlank() }
                }
                RemotePlaylist(id, title, trackIds)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun pushPlay(track: Track, playDurationSec: Int) {
        if (!isSignedIn) return
        runCatching {
            val songId = ensureSongId(track) ?: return
            requestArray("POST", "listening_history", body = JSONObject().apply {
                put("song_id", songId)
                put("play_duration", playDurationSec)
            })
            requestArray("POST", "recently_played", body = JSONObject().put("song_id", songId), prefer = "resolution=merge-duplicates")
        }
    }

    fun clearCache() {
        songIdCache.clear()
        playlistIdCache.clear()
    }

    private fun getArray(table: String, query: String = "select=*"): JSONArray =
        requestArray("GET", table, query)

    private fun requestArray(
        method: String,
        table: String,
        query: String = "",
        body: JSONObject? = null,
        prefer: String? = null,
    ): JSONArray {
        val url = buildString {
            append(Supabase.SUPABASE_URL).append("/rest/v1/").append(table)
            if (query.isNotBlank()) append('?').append(query)
        }
        val builder = Request.Builder().url(url).headers(Supabase.headers(prefer = prefer))
        val requestBody = (body?.toString() ?: "{}").toRequestBody(jsonMedia)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody)
            "PATCH" -> builder.patch(requestBody)
            "DELETE" -> builder.delete()
        }
        client.newCall(builder.build()).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) error(text.ifBlank { res.message })
            return when {
                text.isBlank() -> JSONArray()
                text.trimStart().startsWith("[") -> JSONArray(text)
                else -> JSONArray().put(JSONObject(text))
            }
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun JSONArray.firstObject(): JSONObject? = if (length() > 0) optJSONObject(0) else null
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
}
