package com.beatdrop.app.data.online

/**
 * Online catalogue models — mirrors the approach in
 * github.com/emmanuellaisa00/beatdroppremium (YouTube Innertube, no API key).
 */
data class OnlineResult(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val durationText: String,
    val durationSecs: Int = 0,
    val isLive: Boolean = false,
)

data class OnlineAlbum(
    val browseId: String,
    val audioPlaylistId: String?,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val year: String? = null,
)

data class OnlinePlaylist(
    val playlistId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val trackCountText: String? = null,
)

/** Combined search payload for the Search screen. */
data class OnlineSearchResults(
    val songs: List<OnlineResult> = emptyList(),
    val albums: List<OnlineAlbum> = emptyList(),
    val playlists: List<OnlinePlaylist> = emptyList(),
)

/** A resolved, directly-playable audio stream + the headers it must be fetched with. */
data class ResolvedStream(
    val url: String,
    val userAgent: String,
    val headers: Map<String, String> = emptyMap(),
)
