package com.beatdrop.app.data.model

import android.net.Uri

/** Source of a media item — lets the same UI serve local files now and an online catalogue later. */
enum class MediaSource { LOCAL, ONLINE }

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** Playable URI — content:// for local, https:// for online catalogue. */
    val uri: Uri,
    /** Album-art URI if available (local thumbnail or remote URL); null -> gradient fallback. */
    val artworkUri: Uri? = null,
    /** Stable color key used for the gradient cover fallback (c-1..c-8). */
    val colorKey: String? = null,
    val source: MediaSource = MediaSource.LOCAL,
    /** For ONLINE tracks: the YouTube videoId whose stream is resolved at play time. */
    val onlineId: String? = null,
    /** For resolved ONLINE streams: the exact User-Agent the googlevideo URL needs. */
    val streamUserAgent: String? = null,
    /** For LOCAL tracks: absolute file path (used to find a sidecar .lrc). */
    val filePath: String? = null,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri? = null,
    val colorKey: String? = null,
    val tracks: List<Track> = emptyList(),
    /** Distinguishes an album from a user playlist (same layout, different meta line). */
    val isPlaylist: Boolean = false,
    val year: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val artworkUri: Uri? = null,
    val colorKey: String? = null,
    val trackCount: Int = 0,
    val popularTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
)

/** A horizontal carousel / quick-access entry on Home. */
data class Shelf(
    val title: String,
    val seeAllLabel: String = "See all",
    val items: List<Album>,
)


data class QuickItem(
    val title: String,
    val colorKey: String,
    val albumId: String,
)
