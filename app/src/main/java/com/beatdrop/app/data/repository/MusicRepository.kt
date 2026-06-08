package com.beatdrop.app.data.repository

import android.content.Context
import com.beatdrop.app.data.cloud.CloudSync
import com.beatdrop.app.data.local.LocalMusicSource
import com.beatdrop.app.data.local.PlaylistStore
import com.beatdrop.app.data.local.UserPlaylist
import com.beatdrop.app.data.model.Album
import com.beatdrop.app.data.model.Artist
import com.beatdrop.app.data.model.Shelf
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.DownloadManager
import com.beatdrop.app.data.online.toTrack

/**
 * Single entry point for music data. Today it serves the device's local library.
 * An OnlineCatalogSource can be added behind the same API later — the UI won't change.
 */
class MusicRepository(context: Context) {

    private val local = LocalMusicSource(context.applicationContext)
    private val playlists = PlaylistStore(context.applicationContext)
    // TODO: private val online = OnlineCatalogSource(...)  // wire when catalogue is ready

    // ── User playlists ──
    fun userPlaylists(): List<UserPlaylist> = playlists.all()
    fun createPlaylist(name: String): UserPlaylist = playlists.create(name)
    fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) =
        playlists.addTracks(playlistId, trackIds)
    fun removeTrackFromPlaylist(playlistId: String, trackId: String) =
        playlists.removeTrack(playlistId, trackId)
    fun deletePlaylist(playlistId: String) = playlists.delete(playlistId)

    /** Best-effort cloud mirror for playlist create/rename metadata. */
    suspend fun pushPlaylistMetadata(playlist: UserPlaylist) {
        CloudSync.pushPlaylistMetadata(playlist) { localId, cloudId -> playlists.setCloudId(localId, cloudId) }
    }

    /** Best-effort cloud mirror for adding one track from any global track menu. */
    suspend fun pushPlaylistTrackAdded(playlistId: String, track: Track) {
        val playlist = playlists.byId(playlistId) ?: return
        CloudSync.pushPlaylistTrackAdded(playlist, track) { localId, cloudId -> playlists.setCloudId(localId, cloudId) }
    }

    /** Pushes an exact local playlist snapshot for all resolvable local/downloaded tracks. */
    suspend fun pushPlaylistSnapshot(playlistId: String) {
        val playlist = playlists.byId(playlistId) ?: return
        val tracksById = allResolvableTracks().associateBy { it.id }
        CloudSync.pushPlaylistSnapshot(playlist, tracksById) { localId, cloudId -> playlists.setCloudId(localId, cloudId) }
    }

    /**
     * Merge-on-login playlist sync. Cloud playlists are pulled/merged into the
     * local store first, then the merged local snapshots are pushed back. All
     * cloud failures are swallowed by CloudSync; signed-out users keep a fully
     * local library.
     */
    suspend fun syncCloudPlaylists() {
        if (!CloudSync.isSignedIn) return

        // Pull first so sign-in merges remote + local rows instead of overwriting
        // remote-only tracks when a local playlist has the same title.
        CloudSync.pullPlaylists().forEach { remote ->
            val localMatch = playlists.byCloudId(remote.cloudId)
                ?: playlists.all().firstOrNull { it.cloudId == null && it.name.equals(remote.title, ignoreCase = true) }
            if (localMatch != null) {
                playlists.upsert(
                    localMatch.copy(
                        name = remote.title,
                        trackIds = (localMatch.trackIds + remote.trackIds).distinct(),
                        cloudId = remote.cloudId,
                    )
                )
            } else {
                playlists.upsertCloudPlaylist(remote.cloudId, remote.title, remote.trackIds)
            }
        }

        val tracksById = allResolvableTracks().associateBy { it.id }
        playlists.all().forEach { playlist ->
            CloudSync.pushPlaylistSnapshot(playlist, tracksById) { localId, cloudId ->
                playlists.setCloudId(localId, cloudId)
            }
        }
    }

    private suspend fun allResolvableTracks(): List<Track> =
        (DownloadManager.downloaded.value + local.queryTracks()).distinctBy { it.id }

    /** Resolves a user playlist into a renderable Album with real Track objects. */
    suspend fun playlistAsAlbum(playlistId: String): Album? {
        val pl = playlists.byId(playlistId) ?: return null
        val byId = allResolvableTracks().associateBy { it.id }
        val tracks = pl.trackIds.mapNotNull { byId[it] }
        return Album(
            id = pl.id,
            title = pl.name,
            artist = "You",
            artworkUri = tracks.firstOrNull()?.artworkUri,
            colorKey = tracks.firstOrNull()?.colorKey ?: "c-2",
            tracks = tracks,
            isPlaylist = true,
        )
    }

    suspend fun allTracks(): List<Track> = local.queryTracks()

    suspend fun allAlbums(): List<Album> = local.queryAlbums()

    /** Builds the Home shelves from real library content. */
    suspend fun homeShelves(): List<Shelf> {
        val albums = local.queryAlbums()
        if (albums.isEmpty()) return emptyList()

        val recently = albums.take(10)
        val madeForYou = albums.shuffled().take(10)
        val more = albums.sortedBy { it.title }.take(10)

        return buildList {
            add(Shelf("Recently played", "See all", recently))
            if (madeForYou.isNotEmpty()) add(Shelf("Made for you", "See all", madeForYou))
            if (more.isNotEmpty()) add(Shelf("Your albums", "See all", more))
        }
    }

    suspend fun albumById(id: String): Album? {
        // Online album/playlist — fetch its tracklist from the YouTube engine.
        if (id.startsWith("ytlist_")) {
            val listId = id.removePrefix("ytlist_")
            val items = com.beatdrop.app.data.online.YoutubeService.playlistTracks(listId)
            if (items.isEmpty()) return null
            val tracks = items.map { it.toTrack() }
            val info = com.beatdrop.app.data.online.OnlineListInfo.get(id)
            return Album(
                id = id,
                title = info?.title ?: "Online",
                artist = info?.artist ?: tracks.first().artist,
                artworkUri = info?.artworkUrl?.let { android.net.Uri.parse(it) } ?: tracks.first().artworkUri,
                colorKey = info?.colorKey ?: tracks.first().colorKey,
                tracks = tracks,
                isPlaylist = true,
            )
        }
        return local.queryAlbums().firstOrNull { it.id == id }
    }

    suspend fun allArtists(): List<Artist> = local.queryArtists()

    suspend fun artistByName(name: String): Artist? =
        local.queryArtists().firstOrNull { it.name == name }
}
