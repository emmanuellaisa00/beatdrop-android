package com.beatdrop.app.data.cloud

import com.beatdrop.app.data.local.UserPlaylist
import com.beatdrop.app.data.model.MediaSource
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

/**
 * Local-first, best-effort Supabase sync bridge for the new Spotify-style backend.
 *
 * Important: BeatDrop's catalogue remains external (YouTube/Innertube/local files).
 * Supabase stores user state + `external_items` metadata cache for items the
 * user interacts with; it is NOT a full music catalogue.
 */
object CloudSync {

    val isSignedIn: Boolean get() = Supabase.isAuthenticated()

    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()

    /** external cache key -> external_items.id */
    private val externalItemIdCache = ConcurrentHashMap<String, String>()

    /** local playlist id -> cloud playlists.id */
    private val playlistIdCache = ConcurrentHashMap<String, String>()

    data class RemotePlaylist(
        val cloudId: String,
        val title: String,
        val trackIds: List<String>,
    )

    data class CloudSummary(
        val savedItems: Int = 0,
        val likedItems: Int = 0,
        val followedArtists: Int = 0,
        val playlists: Int = 0,
        val recentPlays: Int = 0,
        val favoriteArtists: List<String> = emptyList(),
    )

    private data class ExternalIdentity(
        val source: String,
        val type: String,
        val id: String,
    ) {
        val key: String = "$source:$type:$id"
    }

    // ───────────────────────────────────────────── external_items ──

    suspend fun ensureExternalItemId(track: Track): String? = withContext(Dispatchers.IO) {
        val identity = track.identity()
        externalItemIdCache[identity.key]?.let { return@withContext it }
        runCatching {
            val existing = getArray(
                "external_items",
                "select=id&external_source=eq.${enc(identity.source)}&external_type=eq.${enc(identity.type)}&external_id=eq.${enc(identity.id)}&limit=1"
            ).firstObject()?.optString("id")?.takeIf { it.isNotBlank() }
            if (existing != null) {
                externalItemIdCache[identity.key] = existing
                return@runCatching existing
            }

            val body = JSONObject().apply {
                put("external_source", identity.source)
                put("external_type", identity.type)
                put("external_id", identity.id)
                put("title", track.title.ifBlank { "Unknown title" })
                put("subtitle", track.artist)
                put("artist_name", track.artist)
                if (track.album.isNotBlank()) put("album_name", track.album)
                put("duration_seconds", (track.durationMs / 1000).toInt())
                track.artworkUri?.let { put("artwork_url", it.toString()) }
                put("metadata_json", JSONObject().apply {
                    put("beatdrop_track_id", track.id)
                    track.onlineId?.let { put("online_id", it) }
                    put("media_source", track.source.name)
                })
            }
            val inserted = requestArray(
                method = "POST",
                table = "external_items",
                query = "on_conflict=external_source,external_type,external_id&select=id",
                body = body,
                prefer = "resolution=merge-duplicates,return=representation"
            ).firstObject()?.optString("id")?.takeIf { it.isNotBlank() }
            if (inserted != null) externalItemIdCache[identity.key] = inserted
            inserted
        }.getOrNull()
    }

    // Backwards-compatible name used by older code paths.
    suspend fun ensureSongId(track: Track): String? = ensureExternalItemId(track)

    // ───────────────────────────────────────────── likes / library ──

    suspend fun pushLike(track: Track, liked: Boolean) {
        if (!isSignedIn) return
        runCatching {
            val userId = Supabase.currentUserId ?: return
            val externalItemId = ensureExternalItemId(track) ?: return
            if (liked) {
                requestArray(
                    "POST",
                    "liked_items",
                    body = JSONObject().apply {
                        put("user_id", userId)
                        put("external_item_id", externalItemId)
                    },
                    prefer = "resolution=merge-duplicates"
                )
                requestArray(
                    "POST",
                    "library_items",
                    body = JSONObject().apply {
                        put("user_id", userId)
                        put("external_item_id", externalItemId)
                        put("item_type", "song")
                    },
                    prefer = "resolution=merge-duplicates"
                )
            } else {
                requestArray("DELETE", "liked_items", "user_id=eq.${enc(userId)}&external_item_id=eq.${enc(externalItemId)}")
            }
        }
    }

    /** Pulls liked BeatDrop track ids from cloud. YouTube song ids become `yt_<videoId>`. */
    suspend fun pullLikedExternalIds(): Set<String> = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext emptySet()
        runCatching {
            val userId = Supabase.currentUserId ?: return@runCatching emptySet<String>()
            getArray(
                "liked_items",
                "select=external_items(external_source,external_type,external_id)&user_id=eq.${enc(userId)}&order=liked_at.desc"
            ).objects().mapNotNull { row ->
                val item = row.optJSONObject("external_items") ?: return@mapNotNull null
                if (item.optString("external_type") != "song") return@mapNotNull null
                item.toBeatDropTrackId()
            }.toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun saveSearch(query: String, category: String? = null) {
        if (!isSignedIn || query.isBlank()) return
        runCatching {
            val userId = Supabase.currentUserId ?: return
            requestArray("POST", "search_history", body = JSONObject().apply {
                put("user_id", userId)
                put("query", query.trim())
                category?.let { put("category", it) }
            })
        }
    }

    suspend fun saveFavoriteArtists(artists: Set<String>) {
        if (!isSignedIn || artists.isEmpty()) return
        runCatching {
            val userId = Supabase.currentUserId ?: return
            val rows = JSONArray()
            artists.map { it.trim() }.filter { it.isNotBlank() }.forEach { artist ->
                rows.put(JSONObject().apply {
                    put("user_id", userId)
                    put("artist_name", artist)
                    put("artist_external_id", artist.lowercase())
                    put("source", "onboarding")
                })
            }
            requestArray("POST", "user_favorite_artists", body = rows, prefer = "resolution=merge-duplicates")
        }
    }

    suspend fun followArtist(artistName: String, artistExternalId: String? = null, artworkUrl: String? = null, follow: Boolean = true) {
        if (!isSignedIn || artistName.isBlank()) return
        runCatching {
            val userId = Supabase.currentUserId ?: return
            val externalId = artistExternalId?.takeIf { it.isNotBlank() } ?: artistName.trim().lowercase()
            if (follow) {
                requestArray("POST", "followed_artists", body = JSONObject().apply {
                    put("user_id", userId)
                    put("artist_external_id", externalId)
                    put("artist_name", artistName.trim())
                    artworkUrl?.let { put("artwork_url", it) }
                }, prefer = "resolution=merge-duplicates")
            } else {
                requestArray("DELETE", "followed_artists", "user_id=eq.${enc(userId)}&artist_external_id=eq.${enc(externalId)}")
            }
        }
    }

    // ───────────────────────────────────────────── playlists ──

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
            val userId = Supabase.currentUserId ?: return@runCatching null
            val body = JSONObject().apply {
                put("user_id", userId)
                put("title", playlist.name)
                put("is_public", false)
                put("is_collaborative", false)
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
            val externalItemId = ensureExternalItemId(track) ?: return
            val position = playlist.trackIds.indexOf(track.id).takeIf { it >= 0 } ?: playlist.trackIds.lastIndex.coerceAtLeast(0)
            requestArray("POST", "playlist_items", body = JSONObject().apply {
                put("playlist_id", cloudPlaylistId)
                put("external_item_id", externalItemId)
                put("position", position)
                Supabase.currentUserId?.let { put("added_by", it) }
            }, prefer = "resolution=merge-duplicates")
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
            requestArray("DELETE", "playlist_items", "playlist_id=eq.${enc(cloudPlaylistId)}")
            playlist.trackIds.forEachIndexed { index, trackId ->
                val track = tracksById[trackId] ?: return@forEachIndexed
                val externalItemId = ensureExternalItemId(track) ?: return@forEachIndexed
                requestArray("POST", "playlist_items", body = JSONObject().apply {
                    put("playlist_id", cloudPlaylistId)
                    put("external_item_id", externalItemId)
                    put("position", index)
                    Supabase.currentUserId?.let { put("added_by", it) }
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
            val userId = Supabase.currentUserId ?: return@runCatching emptyList<RemotePlaylist>()
            getArray(
                "playlists",
                "select=id,title,created_at&user_id=eq.${enc(userId)}&order=created_at.desc"
            ).objects().mapNotNull { row ->
                val id = row.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = row.optString("title", "Cloud Playlist")
                val trackIds = getArray(
                    "playlist_items",
                    "select=position,external_items(external_source,external_type,external_id)&playlist_id=eq.${enc(id)}&order=position.asc"
                ).objects().mapNotNull { item ->
                    item.optJSONObject("external_items")?.toBeatDropTrackId()
                }
                RemotePlaylist(id, title, trackIds)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun pullCloudSummary(): CloudSummary = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext CloudSummary()
        runCatching {
            val userId = Supabase.currentUserId ?: return@runCatching CloudSummary()
            val library = getArray("library_items", "select=external_item_id&user_id=eq.${enc(userId)}").length()
            val liked = getArray("liked_items", "select=external_item_id&user_id=eq.${enc(userId)}").length()
            val followed = getArray("followed_artists", "select=artist_external_id&user_id=eq.${enc(userId)}").length()
            val playlists = getArray("playlists", "select=id&user_id=eq.${enc(userId)}").length()
            val recent = getArray("recently_played", "select=external_item_id&user_id=eq.${enc(userId)}").length()
            val artists = getArray("user_favorite_artists", "select=artist_name&user_id=eq.${enc(userId)}&limit=8")
                .objects()
                .mapNotNull { it.optString("artist_name").takeIf { name -> name.isNotBlank() } }
            CloudSummary(library, liked, followed, playlists, recent, artists)
        }.getOrDefault(CloudSummary())
    }

    // ───────────────────────────────────────────── playback history ──

    suspend fun pushPlay(track: Track, playDurationSec: Int, sourceContext: String? = null, completed: Boolean = false) {
        if (!isSignedIn) return
        runCatching {
            val userId = Supabase.currentUserId ?: return
            val externalItemId = ensureExternalItemId(track) ?: return
            requestArray("POST", "listening_history", body = JSONObject().apply {
                put("user_id", userId)
                put("external_item_id", externalItemId)
                put("play_duration_seconds", playDurationSec)
                put("completed", completed)
                sourceContext?.let { put("source_context", it) }
            })
            requestArray("POST", "recently_played", body = JSONObject().apply {
                put("user_id", userId)
                put("external_item_id", externalItemId)
                put("play_count", 1)
            }, prefer = "resolution=merge-duplicates")
        }
    }

    fun clearCache() {
        externalItemIdCache.clear()
        playlistIdCache.clear()
    }

    // ───────────────────────────────────────────── REST helpers ──

    private fun Track.identity(): ExternalIdentity {
        return if (source == MediaSource.ONLINE) {
            ExternalIdentity("youtube", "song", onlineId ?: id.removePrefix("yt_"))
        } else {
            ExternalIdentity("local", "song", id)
        }
    }

    private fun JSONObject.toBeatDropTrackId(): String? {
        if (optString("external_type") != "song") return null
        val source = optString("external_source")
        val externalId = optString("external_id").takeIf { it.isNotBlank() } ?: return null
        return if (source == "youtube") "yt_$externalId" else externalId
    }

    private fun getArray(table: String, query: String = "select=*"): JSONArray = requestArray("GET", table, query)

    private fun requestArray(
        method: String,
        table: String,
        query: String = "",
        body: Any? = null,
        prefer: String? = null,
    ): JSONArray {
        val url = buildString {
            append(Supabase.SUPABASE_URL).append("/rest/v1/").append(table)
            if (query.isNotBlank()) append('?').append(query)
        }
        val builder = Request.Builder().url(url).headers(Supabase.headers(prefer = prefer))
        val bodyString = when (body) {
            null -> "{}"
            is JSONObject -> body.toString()
            is JSONArray -> body.toString()
            else -> body.toString()
        }
        val requestBody = bodyString.toRequestBody(jsonMedia)
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
