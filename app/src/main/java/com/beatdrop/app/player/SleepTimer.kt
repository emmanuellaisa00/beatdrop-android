package com.beatdrop.app.player

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sleep timer — pauses the bound ExoPlayer when the countdown ends.
 * Exposes remaining millis (0 = off) so the UI can show a live countdown.
 */
object SleepTimer {

    private var player: Player? = null
    private var timer: CountDownTimer? = null
    private val main = Handler(Looper.getMainLooper())

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs

    val isActive: Boolean get() = _remainingMs.value > 0

    fun bind(p: Player) { player = p }
    fun unbind() { cancel(); player = null }

    /** Start (or restart) a timer for [minutes]. */
    fun start(minutes: Int) {
        cancel()
        if (minutes <= 0) return
        val total = minutes * 60_000L
        main.post {
            timer = object : CountDownTimer(total, 1_000L) {
                override fun onTick(msLeft: Long) { _remainingMs.value = msLeft }
                override fun onFinish() {
                    _remainingMs.value = 0L
                    runCatching { player?.pause() }
                }
            }.start()
            _remainingMs.value = total
        }
    }

    fun cancel() {
        main.post {
            timer?.cancel()
            timer = null
            _remainingMs.value = 0L
        }
    }
}
