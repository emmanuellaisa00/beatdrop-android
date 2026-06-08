package com.beatdrop.app.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.beatdrop.app.data.model.Track
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Full ordered queue and the index of the current item. */
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
)

/**
 * Connects the UI to the ExoPlayer running inside PlaybackService via a MediaController.
 * Exposes a StateFlow the Compose layer observes, plus real queue operations.
 */
class PlayerController(private val context: Context) {

    private var controller: MediaController? = null
    /** Mirror of the queue keyed by mediaId so we can rebuild Track objects from the player. */
    private val trackById = HashMap<String, Track>()
    private var pendingQueue: Pair<List<Track>, Int>? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { c ->
                c.addListener(playerListener)
                pendingQueue?.let { (tracks, startIndex) ->
                    pendingQueue = null
                    playQueue(tracks, startIndex)
                } ?: syncFromController()
            }
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromController()
        }
    }

    /** Read the live queue out of the player so reorders/removals reflect immediately. */
    private fun readQueue(c: MediaController): List<Track> {
        val out = ArrayList<Track>(c.mediaItemCount)
        for (i in 0 until c.mediaItemCount) {
            val id = c.getMediaItemAt(i).mediaId
            trackById[id]?.let { out.add(it) }
        }
        return out
    }

    private fun syncFromController() {
        val c = controller ?: return
        val queue = readQueue(c)
        val idx = c.currentMediaItemIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
        val track = queue.getOrNull(idx)
        _state.value = PlaybackState(
            current = track,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = if (c.duration > 0) c.duration else (track?.durationMs ?: 0L),
            queue = queue,
            currentIndex = idx,
            shuffle = c.shuffleModeEnabled,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
        )
    }

    /** Position changes don't always fire events; UI can poll this. */
    fun currentPosition(): Long = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: run {
            pendingQueue = tracks to startIndex
            connect()
            return
        }
        tracks.forEach { trackById[it.id] = it }
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    // ── Queue operations (real ExoPlayer) ──

    /** Jump to a specific queue position. */
    fun jumpTo(index: Int) {
        val c = controller ?: return
        c.seekToDefaultPosition(index)
        c.play()
    }

    /** Insert right after the current item ("Play next"). */
    fun playNext(track: Track) {
        val c = controller ?: return
        trackById[track.id] = track
        c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem())
    }

    /** Append to the end of the queue ("Add to queue"). */
    fun addToQueue(track: Track) {
        val c = controller ?: return
        trackById[track.id] = track
        c.addMediaItem(track.toMediaItem())
    }

    /** Reorder within the queue (drag handle). */
    fun moveItem(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }

    fun removeItem(index: Int) {
        controller?.removeMediaItem(index)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}

private fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .build()
        )
        .build()
