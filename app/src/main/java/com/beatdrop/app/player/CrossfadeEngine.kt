package com.beatdrop.app.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import com.beatdrop.app.data.local.CrossfadeSettings

/**
 * Lightweight crossfade for ExoPlayer.
 *
 * ExoPlayer has no native crossfade, so we approximate it: a 250ms ticker watches
 * the current item's remaining time and, within the crossfade window, ramps the
 * player volume down to 0 just before the automatic transition; when the next item
 * starts it ramps volume back up from 0. The result is an audible fade-out/fade-in
 * (a "gap-bridging" crossfade) without a second player instance.
 *
 * Honors [CrossfadeSettings.seconds] (0 disables). Seeks/pauses reset volume to 1.
 */
class CrossfadeEngine(private val player: Player) {

    private val handler = Handler(Looper.getMainLooper())
    private var lastIndex = -1
    private var fadingIn = false
    private var fadeInStartAt = 0L

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    fun start() { handler.post(ticker) }
    fun stop() {
        handler.removeCallbacks(ticker)
        runCatching { player.volume = 1f }
    }

    private fun tick() {
        val xfMs = CrossfadeSettings.seconds * 1000L
        if (xfMs <= 0L) {
            if (player.volume != 1f) player.volume = 1f
            return
        }
        if (!player.isPlaying && !fadingIn) return

        val idx = player.currentMediaItemIndex
        if (idx != lastIndex) {
            // New item started — begin fade-in from 0.
            lastIndex = idx
            fadingIn = true
            fadeInStartAt = System.currentTimeMillis()
            player.volume = 0f
            return
        }

        // Fade-in phase right after a transition.
        if (fadingIn) {
            val elapsed = System.currentTimeMillis() - fadeInStartAt
            val v = (elapsed.toFloat() / xfMs).coerceIn(0f, 1f)
            player.volume = v
            if (v >= 1f) fadingIn = false
            return
        }

        // Fade-out phase near the end of the current item.
        val dur = player.duration
        if (dur <= 0L) { if (player.volume != 1f) player.volume = 1f; return }
        val remaining = dur - player.currentPosition
        // Only fade out when there IS a next item to fade into.
        val hasNext = player.hasNextMediaItem()
        if (hasNext && remaining in 1..xfMs) {
            player.volume = (remaining.toFloat() / xfMs).coerceIn(0f, 1f)
        } else if (player.volume != 1f) {
            player.volume = 1f
        }
    }

    companion object { private const val TICK_MS = 250L }
}
