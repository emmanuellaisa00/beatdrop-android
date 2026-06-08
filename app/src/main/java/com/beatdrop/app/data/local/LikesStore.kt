package com.beatdrop.app.data.local

import android.content.Context
import com.beatdrop.app.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real on-device "Liked Songs" store (SharedPreferences/JSON).
 * Persists the full Track payload so liked online tracks survive restarts
 * (and stay playable) without re-querying the catalogue.
 */
class LikesStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("beatdrop_likes", Context.MODE_PRIVATE)

    private val _liked = MutableStateFlow(load())
    val liked: StateFlow<List<Track>> = _liked

    val likedIds: Set<String> get() = _liked.value.map { it.id }.toSet()

    fun isLiked(id: String): Boolean = _liked.value.any { it.id == id }

    fun toggle(track: Track) {
        val current = _liked.value
        val next = if (current.any { it.id == track.id }) {
            current.filterNot { it.id == track.id }
        } else {
            listOf(track) + current // most-recent first
        }
        save(next)
        _liked.value = next
    }

    private fun load(): List<Track> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<Track>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(t: Track) = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("artist", t.artist)
        put("album", t.album)
        put("durationMs", t.durationMs)
        put("uri", t.uri.toString())
        put("artworkUri", t.artworkUri?.toString())
        put("colorKey", t.colorKey)
        put("source", t.source.name)
        put("onlineId", t.onlineId)
        put("filePath", t.filePath)
    }

    private fun fromJson(o: JSONObject) = Track(
        id = o.getString("id"),
        title = o.optString("title"),
        artist = o.optString("artist"),
        album = o.optString("album"),
        durationMs = o.optLong("durationMs"),
        uri = android.net.Uri.parse(o.optString("uri")),
        artworkUri = o.optString("artworkUri", null)?.let(android.net.Uri::parse),
        colorKey = o.optString("colorKey", null),
        source = runCatching { com.beatdrop.app.data.model.MediaSource.valueOf(o.optString("source")) }
            .getOrDefault(com.beatdrop.app.data.model.MediaSource.LOCAL),
        onlineId = o.optString("onlineId", null),
        filePath = o.optString("filePath", null),
    )

    companion object { private const val KEY = "likes_json" }
}
