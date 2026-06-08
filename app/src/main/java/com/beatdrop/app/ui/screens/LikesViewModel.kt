package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatdrop.app.data.cloud.CloudSync
import com.beatdrop.app.data.local.LikesStore
import com.beatdrop.app.data.model.Track
import com.beatdrop.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * App-wide Liked Songs state (local-first). Toggles write locally for instant UX
 * and mirror to Supabase when signed in; on sign-in, cloud likes merge into local.
 */
class LikesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LikesStore(app)
    private val repo = MusicRepository(app)

    val liked: StateFlow<List<Track>> = store.liked

    fun isLiked(id: String): Boolean = store.isLiked(id)

    fun toggle(track: Track) {
        val nowLiked = store.toggle(track)               // local-first
        viewModelScope.launch { CloudSync.pushLike(track, nowLiked) } // best-effort mirror
    }

    /** Called after sign-in: merge cloud likes into local, then push local-only likes up. */
    fun syncOnSignIn() {
        viewModelScope.launch {
            // Pull cloud → merge into local (resolve ids against the device library + current likes).
            val cloudIds = CloudSync.pullLikedExternalIds()
            if (cloudIds.isNotEmpty()) {
                val lookup = buildMap {
                    store.liked.value.forEach { put(it.id, it) }
                    runCatching { repo.allTracks() }.getOrDefault(emptyList()).forEach { put(it.id, it) }
                }
                store.mergeLikedIds(cloudIds) { lookup[it] }
            }
            // Push local likes the cloud doesn't have yet.
            store.liked.value.forEach { t ->
                if (t.id !in cloudIds) CloudSync.pushLike(t, true)
            }
        }
    }
}
