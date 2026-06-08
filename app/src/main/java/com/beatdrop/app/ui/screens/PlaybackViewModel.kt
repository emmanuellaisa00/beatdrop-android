package com.beatdrop.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.OnlinePlaybackDebugLog
import com.beatdrop.app.data.online.YoutubeService
import com.beatdrop.app.player.PlaybackState
import com.beatdrop.app.player.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shared player VM: connects to the real ExoPlayer (via PlayerController/MediaController),
 * exposes playback state to all screens, and polls position for the progress bar.
 */
class PlaybackViewModel(app: Application) : AndroidViewModel(app) {

    private val controller = PlayerController(app)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    /** True while online stream URLs are being resolved before playback starts. */
    private val _resolving = MutableStateFlow(false)
    val resolving: StateFlow<Boolean> = _resolving

    private val _resolveError = MutableStateFlow<String?>(null)
    val resolveError: StateFlow<String?> = _resolveError
    val debugLog: StateFlow<List<String>> = OnlinePlaybackDebugLog.lines

    private var resolveJob: Job? = null
    private var lastOnlineRequest: Pair<List<Track>, Int>? = null

    init {
        controller.connect()
        viewModelScope.launch {
            controller.state.collect { s ->
                // Do not overwrite the pending online Now Playing shell while resolving.
                if (!_resolving.value) _state.value = s
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(250)
                if (_state.value.isPlaying) {
                    _state.update { it.copy(positionMs = controller.currentPosition()) }
                }
            }
        }
    }

    /**
     * Play a queue. Local tracks play directly. ONLINE tracks open instantly as a
     * pending Now Playing item, then resolve their stream in the background.
     */
    fun play(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val start = tracks[startIndex]
        viewModelScope.launch {
            com.beatdrop.app.data.cloud.CloudSync.pushPlay(start, (start.durationMs / 1000).toInt())
        }
        if (start.source != MediaSource.ONLINE) {
            resolveJob?.cancel()
            _resolving.value = false
            _resolveError.value = null
            OnlinePlaybackDebugLog.add("Local play: ${start.title} (${start.id})")
            controller.playQueue(tracks, startIndex)
            return
        }

        lastOnlineRequest = tracks to startIndex
        resolveJob?.cancel()
        _resolveError.value = null
        OnlinePlaybackDebugLog.clear()
        OnlinePlaybackDebugLog.add("Tap online song: title='${start.title}', artist='${start.artist}', trackId='${start.id}', onlineId='${start.onlineId}', queueSize=${tracks.size}, startIndex=$startIndex")
        _state.value = PlaybackState(
            current = start,
            isPlaying = false,
            durationMs = start.durationMs,
            queue = tracks,
            currentIndex = startIndex,
        )

        resolveJob = viewModelScope.launch {
            _resolving.value = true
            OnlinePlaybackDebugLog.add("Resolve start: timeout=25000ms")
            val resolvedStart = withTimeoutOrNull(25_000) { resolve(start) }
            if (resolvedStart == null) {
                _resolving.value = false
                _resolveError.value = "Couldn’t load this song. Check connection or tap Retry."
                OnlinePlaybackDebugLog.add("Resolve failed: no stream returned before timeout or all strategies failed")
                return@launch
            }
            OnlinePlaybackDebugLog.add("Resolve success: uriHost='${resolvedStart.uri.host}', userAgent='${resolvedStart.streamUserAgent.orEmpty().take(80)}'")
            controller.playQueue(listOf(resolvedStart), 0)
            OnlinePlaybackDebugLog.add("Submitted resolved track to MediaController")
            _resolving.value = false
            _resolveError.value = null

            OnlinePlaybackDebugLog.add("Queue pre-resolution deferred: selected track must reach stable playback first. queueSize=${tracks.size}")
        }
    }

    fun retryOnline() {
        val (tracks, index) = lastOnlineRequest ?: run {
            OnlinePlaybackDebugLog.add("Retry ignored: no last online request")
            return
        }
        OnlinePlaybackDebugLog.add("Retry requested")
        play(tracks, index)
    }

    private suspend fun resolve(track: Track): Track? {
        if (track.source != MediaSource.ONLINE) return track
        val id = track.onlineId ?: run {
            OnlinePlaybackDebugLog.add("Resolve failed: track has no onlineId")
            return null
        }
        OnlinePlaybackDebugLog.add("YoutubeService.getStream('$id', bypassCache=true)")
        val stream = YoutubeService.getStream(id, bypassCache = true) ?: return null
        return track.copy(uri = Uri.parse(stream.url), streamUserAgent = stream.userAgent)
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(ms: Long) = controller.seekTo(ms)

    fun jumpTo(index: Int) = controller.jumpTo(index)
    fun playNext(track: Track) = controller.playNext(track)
    fun addToQueue(track: Track) = controller.addToQueue(track)
    fun moveQueueItem(from: Int, to: Int) = controller.moveItem(from, to)
    fun removeQueueItem(index: Int) = controller.removeItem(index)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeat() = controller.cycleRepeat()

    override fun onCleared() {
        resolveJob?.cancel()
        controller.release()
        super.onCleared()
    }
}

fun Long.formatTime(): String {
    if (this <= 0) return "0:00"
    val totalSec = this / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
