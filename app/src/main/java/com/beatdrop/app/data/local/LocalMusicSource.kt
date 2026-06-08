package com.beatdrop.app.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Artist
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's real music library via MediaStore — no mock data.
 * Requires READ_MEDIA_AUDIO (API 33+) / READ_EXTERNAL_STORAGE (<=32).
 */
class LocalMusicSource(private val context: Context) {

    suspend fun queryTracks(): List<Track> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )
                result += Track(
                    id = id.toString(),
                    title = c.getString(titleCol) ?: "Unknown",
                    artist = c.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                    album = c.getString(albumCol) ?: "Unknown album",
                    durationMs = c.getLong(durCol),
                    uri = contentUri,
                    artworkUri = artUri,
                    colorKey = "c-${(albumId % 8 + 1)}",
                    source = MediaSource.LOCAL,
                    filePath = if (dataCol >= 0) c.getString(dataCol) else null,
                )
            }
        }
        result
    }

    suspend fun queryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        queryTracks()
            .groupBy { it.album }
            .map { (albumName, tracks) ->
                val first = tracks.first()
                Album(
                    id = albumName,
                    title = albumName,
                    artist = first.artist,
                    artworkUri = first.artworkUri,
                    colorKey = first.colorKey,
                    tracks = tracks,
                )
            }
    }

    suspend fun queryArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val tracks = queryTracks()
        tracks
            .groupBy { it.artist }
            .map { (name, artistTracks) ->
                val albums = artistTracks
                    .groupBy { it.album }
                    .map { (albumName, albumTracks) ->
                        val f = albumTracks.first()
                        Album(albumName, albumName, name, f.artworkUri, f.colorKey, albumTracks)
                    }
                Artist(
                    id = name,
                    name = name,
                    artworkUri = artistTracks.first().artworkUri,
                    colorKey = artistTracks.first().colorKey,
                    trackCount = artistTracks.size,
                    popularTracks = artistTracks.take(5),
                    albums = albums,
                )
            }
    }
}
