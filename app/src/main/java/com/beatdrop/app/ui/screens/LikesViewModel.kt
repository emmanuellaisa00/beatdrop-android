package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.beatdrop.app.data.local.LikesStore
import com.beatdrop.app.data.model.Track
import kotlinx.coroutines.flow.StateFlow

/** App-wide Liked Songs state. Hoisted in BeatDropApp so every screen shares it. */
class LikesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LikesStore(app)

    val liked: StateFlow<List<Track>> = store.liked

    fun isLiked(id: String): Boolean = store.isLiked(id)
    fun toggle(track: Track) = store.toggle(track)
}
