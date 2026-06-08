package com.beatdrop.app.data.online

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copyable in-app debug log for online playback failures.
 * Kept small and in memory; safe to show/copy from Now Playing.
 */
object OnlinePlaybackDebugLog {
    private const val TAG = "BeatDropOnline"
    private const val MAX_LINES = 140
    private val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun clear() {
        _lines.value = emptyList()
        Log.d(TAG, "clear")
    }

    fun add(message: String, error: Throwable? = null) {
        val line = "${clock.format(Date())}  $message${error?.let { " — ${it::class.java.simpleName}: ${it.message}" }.orEmpty()}"
        _lines.value = (_lines.value + line).takeLast(MAX_LINES)
        if (error == null) Log.d(TAG, message) else Log.e(TAG, message, error)
    }

    fun text(): String = _lines.value.joinToString("\n")
}
