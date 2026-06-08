package com.beatdrop.app.data.online

import android.net.Uri
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track

/**
 * Converts online catalogue results into the app's Track/Album models.
 * Online tracks start with a placeholder URI; the real googlevideo stream is
 * resolved lazily (YoutubeService.getStream) right before playback.
 */

private fun colorForId(id: String): String = "c-${(Math.abs(id.hashCode()) % 8) + 1}"

/** Remembers display info for online lists so the detail screen shows the right
 *  title/artist/art even though it re-fetches the tracklist by id. */
object OnlineListInfo {
    data class Info(val title: String, val artist: String, val artworkUrl: String?, val colorKey: String)
    private val map = HashMap<String, Info>()
    fun put(id: String, info: Info) { map[id] = info }
    fun get(id: String): Info? = map[id]
}

fun OnlineResult.toTrack(): Track = Track(
    id = "yt_$videoId",
    title = title,
    artist = author.ifBlank { "Unknown artist" },
    album = "",
    durationMs = durationSecs * 1000L,
    uri = Uri.EMPTY, // resolved at play time
    artworkUri = thumbnailUrl?.let(Uri::parse),
    colorKey = colorForId(videoId),
    source = MediaSource.ONLINE,
    onlineId = videoId,
)

// Online albums/playlists encode the playable list id in the Album.id so the
// detail screen can fetch their tracks via YoutubeService.playlistTracks().
//   ytlist_<audioPlaylistId|playlistId>

fun OnlineAlbum.toAlbum(): Album {
    val id = "ytlist_${audioPlaylistId ?: browseId}"
    OnlineListInfo.put(id, OnlineListInfo.Info(title, artist.ifBlank { "Unknown artist" }, thumbnailUrl, colorForId(browseId)))
    return Album(
        id = id,
        title = title,
        artist = artist.ifBlank { "Unknown artist" },
        artworkUri = thumbnailUrl?.let(Uri::parse),
        colorKey = colorForId(browseId),
    )
}

fun OnlinePlaylist.toAlbum(): Album {
    val id = "ytlist_$playlistId"
    OnlineListInfo.put(id, OnlineListInfo.Info(title, author.ifBlank { "Playlist" }, thumbnailUrl, colorForId(playlistId)))
    return Album(
        id = id,
        title = title,
        artist = author.ifBlank { "Playlist" },
        artworkUri = thumbnailUrl?.let(Uri::parse),
        colorKey = colorForId(playlistId),
        isPlaylist = true,
    )
}
