package com.beatdrop.app.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A lightweight, real on-device store for user-created playlists (SharedPreferences/JSON). */
data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

class PlaylistStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("beatdrop_playlists", Context.MODE_PRIVATE)

    fun all(): List<UserPlaylist> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ids = o.optJSONArray("trackIds") ?: JSONArray()
                add(
                    UserPlaylist(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        trackIds = (0 until ids.length()).map { ids.getString(it) },
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    fun create(name: String): UserPlaylist {
        val playlist = UserPlaylist(id = "pl_${System.currentTimeMillis()}", name = name)
        save(all() + playlist)
        return playlist
    }

    fun byId(id: String): UserPlaylist? = all().firstOrNull { it.id == id }

    /** Adds tracks (de-duplicated, order preserved) to a playlist and persists. */
    fun addTracks(playlistId: String, trackIds: List<String>) {
        val list = all().map { p ->
            if (p.id == playlistId) {
                val merged = (p.trackIds + trackIds).distinct()
                p.copy(trackIds = merged)
            } else p
        }
        save(list)
    }

    fun removeTrack(playlistId: String, trackId: String) {
        val list = all().map { p ->
            if (p.id == playlistId) p.copy(trackIds = p.trackIds - trackId) else p
        }
        save(list)
    }

    fun delete(playlistId: String) {
        save(all().filterNot { it.id == playlistId })
    }

    private fun save(list: List<UserPlaylist>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("createdAt", p.createdAt)
                    put("trackIds", JSONArray(p.trackIds))
                }
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object { private const val KEY = "playlists_json" }
}
