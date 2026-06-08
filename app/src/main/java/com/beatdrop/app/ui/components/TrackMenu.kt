package com.beatdrop.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.beatdrop.app.data.model.Track

/**
 * App-wide hook for opening the global track ⋯ context sheet. Provided once in
 * BeatDropApp; any TrackRow calls it from its "more" button without each screen
 * having to thread the callback down.
 */
val LocalTrackMenu = staticCompositionLocalOf<(Track) -> Unit> { {} }

/** App-wide "is this track liked?" lookup, for hearts/affordances in rows. */
val LocalIsLiked = staticCompositionLocalOf<(String) -> Boolean> { { false } }

/** App-wide download trigger for online tracks (enqueue into DownloadManager). */
val LocalDownload = staticCompositionLocalOf<(Track) -> Unit> { {} }
val LocalCancelDownload = staticCompositionLocalOf<(String) -> Unit> { {} }

/** App-wide "open profile" hook (avatar tap in any Hero header). */
val LocalOpenProfile = staticCompositionLocalOf<() -> Unit> { {} }
