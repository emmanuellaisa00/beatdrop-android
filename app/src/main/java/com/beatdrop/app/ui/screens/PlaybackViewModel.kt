package com.beatdrop.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.online.YoutubeService
import com.beatdrop.app.player.PlaybackState
import com.beatdrop.app.player.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init {
        controller.connect()
        // Mirror controller state
        viewModelScope.launch {
            controller.state.collect { s -> _state.value = s }
        }
        // Poll position ~4x/sec so the progress bar advances smoothly
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
     * Play a queue. Local tracks play directly. For ONLINE tracks we resolve the
     * starting track's googlevideo stream first (so playback begins fast), then
     * resolve the rest in the background and patch them into the live queue.
     */
    fun play(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val start = tracks[startIndex]
        // Record the play to the cloud (recently-played + history) — best-effort, no-op if signed out.
        viewModelScope.launch {
            com.beatdrop.app.data.cloud.CloudSync.pushPlay(start, (start.durationMs / 1000).toInt())
        }
        if (start.source != MediaSource.ONLINE) {
            controller.playQueue(tracks, startIndex)
            return
        }
        viewModelScope.launch {
            _resolving.value = true
            val resolvedStart = withTimeoutOrNull(25_000) { resolve(start) }
            if (resolvedStart == null) { _resolving.value = false; return@launch }
            // Start immediately with just the resolved track…
            controller.playQueue(listOf(resolvedStart), 0)
            _resolving.value = false
            // …then resolve the remainder and append, preserving order.
            val rest = tracks.filterIndexed { i, _ -> i != startIndex }
            rest.forEach { t ->
                withTimeoutOrNull(20_000) { resolve(t) }?.let { controller.addToQueue(it) }
            }
        }
    }

    private suspend fun resolve(track: Track): Track? {
        if (track.source != MediaSource.ONLINE) return track
        val id = track.onlineId ?: return null
        val stream = YoutubeService.getStream(id) ?: return null
        return track.copy(uri = Uri.parse(stream.url), streamUserAgent = stream.userAgent)
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(ms: Long) = controller.seekTo(ms)

    // Queue operations
    fun jumpTo(index: Int) = controller.jumpTo(index)
    fun playNext(track: Track) = controller.playNext(track)
    fun addToQueue(track: Track) = controller.addToQueue(track)
    fun moveQueueItem(from: Int, to: Int) = controller.moveItem(from, to)
    fun removeQueueItem(index: Int) = controller.removeItem(index)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeat() = controller.cycleRepeat()

    override fun onCleared() {
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
