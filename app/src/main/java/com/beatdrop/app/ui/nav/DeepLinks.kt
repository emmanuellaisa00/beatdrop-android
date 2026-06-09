package com.beatdrop.app.ui.nav

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Simple process-wide deep-link handoff from MainActivity into the Compose navigator. */
object BeatDropDeepLinks {
    var pendingPlaylistId by mutableStateOf<String?>(null)
        private set

    fun handle(uri: Uri?) {
        if (uri == null || uri.scheme != "beatdrop") return
        when (uri.host) {
            "playlist" -> {
                pendingPlaylistId = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
            }
        }
    }

    fun consumePlaylist(): String? = pendingPlaylistId.also { pendingPlaylistId = null }
}
