package com.beatdrop.app.data.cloud

import com.beatdrop.app.data.local.UserPlaylist
import com.beatdrop.app.data.model.Track
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges BeatDrop's local-first data to the Supabase backend.
 *
 * Design rules:
 *  - **Local-first**: callers always write locally first; these methods mirror to
 *    the cloud only when signed in. Every call is best-effort (wrapped in
 *    runCatching) so a cloud/RLS/permission failure never breaks local playback.
 *  - **ID mapping**: BeatDrop tracks use MediaStore ids / "yt_<videoId>". The cloud
 *    `songs` table keys on a UUID, with `external_source_id` for the foreign id.
 *    [ensureSongId] resolves (or lazily creates) the cloud song row for a Track.
 */
object CloudSync {

    private val likedTable get() = Supabase.postgrest.from("liked_songs")
    private val songsTable get() = Supabase.postgrest.from("songs")
    private val historyTable get() = Supabase.postgrest.from("listening_history")
    private val recentTable get() = Supabase.postgrest.from("recently_played")
    private val playlistsTable get() = Supabase.postgrest.from("playlists")
    private val playlistSongsTable get() = Supabase.postgrest.from("playlist_songs")

    val isSignedIn: Boolean get() = Supabase.isAuthenticated()

    /** externalId (track.id) -> cloud songs.id */
    private val songIdCache = ConcurrentHashMap<String, String>()

    /** local playlist id -> cloud playlists.id */
    private val playlistIdCache = ConcurrentHashMap<String, String>()

    /** Pulled cloud playlist payload, expressed in BeatDrop track ids. */
    data class RemotePlaylist(
        val cloudId: String,
        val title: String,
        val trackIds: List<String>,
    )

    /**
     * Returns the cloud `songs.id` for a track, creating the catalog row if needed.
     * Returns null if the catalog isn't reachable/writable (caller treats as no-op).
     */
    suspend fun ensureSongId(track: Track): String? = withContext(Dispatchers.IO) {
        songIdCache[track.id]?.let { return@withContext it }
        runCatching {
            // 1) Look up by external_source_id.
            val existing = songsTable.select(Columns.list("id", "external_source_id")) {
                filter { eq("external_source_id", track.id) }
                limit(count = 1)
            }.decodeList<JsonObject>().firstOrNull()
            val foundId = existing?.get("id")?.jsonPrimitive?.contentOrNull
            if (foundId != null) {
                songIdCache[track.id] = foundId
                return@runCatching foundId
            }
            // 2) Insert a new catalog row (best-effort; may be blocked by RLS).
            val inserted = songsTable.insert(
                buildJsonObject {
                    put("title", track.title)
                    put("artist_name", track.artist)
                    if (track.album.isNotBlank()) put("album_name", track.album)
                    put("duration", (track.durationMs / 1000).toInt())
                    track.artworkUri?.let { put("artwork_url", it.toString()) }
                    put("external_source_id", track.id)
                }
            ) { select(Columns.list("id")) }.decodeList<JsonObject>().firstOrNull()
            val newId = inserted?.get("id")?.jsonPrimitive?.contentOrNull
            if (newId != null) songIdCache[track.id] = newId
            newId
        }.getOrNull()
    }

    // ── Likes ──

    suspend fun pushLike(track: Track, liked: Boolean) {
        if (!isSignedIn) return
        runCatching {
            val songId = ensureSongId(track) ?: return
            if (liked) {
                likedTable.insert(buildJsonObject { put("song_id", songId) })
            } else {
                likedTable.delete { filter { eq("song_id", songId) } }
            }
        }
    }

    /** Pulls the set of liked external ids (track.id) from the cloud. */
    suspend fun pullLikedExternalIds(): Set<String> = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext emptySet()
        runCatching {
            // liked_songs joined to songs to recover external_source_id.
            val rows = likedTable.select(Columns.raw("song_id, songs(external_source_id)")) {
                order("liked_at", Order.DESCENDING)
            }.decodeList<JsonObject>()
            rows.mapNotNull { row ->
                when (val s = row["songs"]) {
                    is JsonObject -> s["external_source_id"]?.jsonPrimitive?.contentOrNull
                    is JsonArray ->
                        (s.firstOrNull() as? JsonObject)?.get("external_source_id")?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            }.toSet()
        }.getOrDefault(emptySet())
    }

    // ── Playlists ──

    /** Ensures a cloud playlist row exists for a local playlist. */
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
            val inserted = playlistsTable.insert(
                buildJsonObject {
                    put("title", playlist.name)
                    put("is_public", false)
                }
            ) { select(Columns.list("id")) }.decodeList<JsonObject>().firstOrNull()
            val cloudId = inserted?.get("id")?.jsonPrimitive?.contentOrNull
            if (cloudId != null) {
                playlistIdCache[playlist.id] = cloudId
                onCloudId(playlist.id, cloudId)
            }
            cloudId
        }.getOrNull()
    }

    /** Mirrors playlist title/visibility without touching its songs. */
    suspend fun pushPlaylistMetadata(
        playlist: UserPlaylist,
        onCloudId: (localId: String, cloudId: String) -> Unit = { _, _ -> },
    ) {
        if (!isSignedIn) return
        runCatching {
            val cloudId = ensurePlaylistId(playlist, onCloudId) ?: return
            playlistsTable.update(
                buildJsonObject { put("title", playlist.name) }
            ) { filter { eq("id", cloudId) } }
        }
    }

    /** Adds one locally-added track to its cloud playlist without rewriting the whole playlist. */
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
            playlistSongsTable.insert(
                buildJsonObject {
                    put("playlist_id", cloudPlaylistId)
                    put("song_id", songId)
                    put("position", position)
                }
            )
        }
    }

    /** Replaces the cloud membership with the exact local order for known tracks. */
    suspend fun pushPlaylistSnapshot(
        playlist: UserPlaylist,
        tracksById: Map<String, Track>,
        onCloudId: (localId: String, cloudId: String) -> Unit = { _, _ -> },
    ) {
        if (!isSignedIn) return
        runCatching {
            val cloudPlaylistId = ensurePlaylistId(playlist, onCloudId) ?: return
            playlistsTable.update(buildJsonObject { put("title", playlist.name) }) {
                filter { eq("id", cloudPlaylistId) }
            }
            playlistSongsTable.delete { filter { eq("playlist_id", cloudPlaylistId) } }
            playlist.trackIds.forEachIndexed { index, trackId ->
                val track = tracksById[trackId] ?: return@forEachIndexed
                val songId = ensureSongId(track) ?: return@forEachIndexed
                playlistSongsTable.insert(
                    buildJsonObject {
                        put("playlist_id", cloudPlaylistId)
                        put("song_id", songId)
                        put("position", index)
                    }
                )
            }
        }
    }

    suspend fun deleteCloudPlaylist(cloudId: String?) {
        if (!isSignedIn || cloudId == null) return
        runCatching { playlistsTable.delete { filter { eq("id", cloudId) } } }
    }

    /** Pulls cloud playlists and their song external ids for merge-on-login. */
    suspend fun pullPlaylists(): List<RemotePlaylist> = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext emptyList()
        runCatching {
            val rows = playlistsTable.select(Columns.list("id", "title", "created_at")) {
                Supabase.currentUserId?.let { filter { eq("user_id", it) } }
                order("created_at", Order.DESCENDING)
            }.decodeList<JsonObject>()
            rows.mapNotNull { row ->
                val id = row["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = row["title"]?.jsonPrimitive?.contentOrNull ?: "Cloud Playlist"
                val songRows = playlistSongsTable.select(Columns.raw("position, songs(external_source_id)")) {
                    filter { eq("playlist_id", id) }
                    order("position", Order.ASCENDING)
                }.decodeList<JsonObject>()
                val trackIds = songRows.mapNotNull { songRow ->
                    when (val s = songRow["songs"]) {
                        is JsonObject -> s["external_source_id"]?.jsonPrimitive?.contentOrNull
                        is JsonArray ->
                            (s.firstOrNull() as? JsonObject)?.get("external_source_id")?.jsonPrimitive?.contentOrNull
                        else -> null
                    }
                }
                RemotePlaylist(cloudId = id, title = title, trackIds = trackIds)
            }
        }.getOrDefault(emptyList())
    }

    // ── History / recently played ──

    suspend fun pushPlay(track: Track, playDurationSec: Int) {
        if (!isSignedIn) return
        runCatching {
            val songId = ensureSongId(track) ?: return
            historyTable.insert(buildJsonObject {
                put("song_id", songId)
                put("play_duration", playDurationSec)
            })
            recentTable.upsert(
                buildJsonObject { put("song_id", songId) },
                onConflict = "user_id,song_id"
            )
        }
    }

    fun clearCache() {
        songIdCache.clear()
        playlistIdCache.clear()
    }
}
