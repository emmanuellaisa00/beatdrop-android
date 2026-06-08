package com.beatdrop.app.ui.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A pushable detail destination (album/playlist or artist). */
sealed interface Destination {
    data class AlbumDetail(val albumId: String) : Destination
    data class ArtistDetail(val artistName: String) : Destination
    data class PlaylistDetail(val playlistId: String) : Destination
    data object LikedSongs : Destination
    data object Downloads : Destination
    data object Profile : Destination
    data object Settings : Destination
    data object Equalizer : Destination
}

/**
 * Spotify-iOS-style navigation:
 *  - 4 tabs, each with its OWN independent back stack
 *  - detail screens push onto the *current* tab's stack
 *  - switching tabs preserves each tab's stack
 *  - the mini player + tab bar persist above everything
 */
@Stable
class Navigator {
    var currentTab by mutableStateOf(Tab.Library)
        private set

    // one back stack per tab
    private val stacks = mutableStateMapOf<Tab, List<Destination>>().apply {
        Tab.entries.forEach { put(it, emptyList()) }
    }

    /** Top destination of the current tab, or null when at the tab root. */
    val currentDestination: Destination?
        get() = stacks[currentTab]?.lastOrNull()

    fun stackFor(tab: Tab): List<Destination> = stacks[tab] ?: emptyList()

    fun selectTab(tab: Tab) {
        if (tab == currentTab) {
            // tapping the active tab pops to its root (iOS behavior)
            stacks[tab] = emptyList()
        } else {
            currentTab = tab
        }
    }

    fun push(destination: Destination) {
        stacks[currentTab] = stackFor(currentTab) + destination
    }

    /** Returns true if a detail screen was popped (so the system back is consumed). */
    fun pop(): Boolean {
        val stack = stackFor(currentTab)
        if (stack.isEmpty()) return false
        stacks[currentTab] = stack.dropLast(1)
        return true
    }

    val canPop: Boolean get() = stackFor(currentTab).isNotEmpty()
}
