package com.beatdrop.app.data.cloud

import com.beatdrop.app.data.model.Track
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
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

    val isSignedIn: Boolean get() = Supabase.isAuthenticated()

    /** externalId (track.id) -> cloud songs.id */
    private val songIdCache = ConcurrentHashMap<String, String>()

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
                order("liked_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<JsonObject>()
            rows.mapNotNull { row ->
                when (val s = row["songs"]) {
                    is JsonObject -> s["external_source_id"]?.jsonPrimitive?.contentOrNull
                    is kotlinx.serialization.json.JsonArray ->
                        (s.firstOrNull() as? JsonObject)?.get("external_source_id")?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            }.toSet()
        }.getOrDefault(emptySet())
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

    fun clearCache() = songIdCache.clear()
}
