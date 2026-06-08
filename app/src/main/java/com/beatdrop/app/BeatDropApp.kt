package com.beatdrop.app

import android.app.Application
import com.beatdrop.app.data.online.DownloadManager
import com.beatdrop.app.data.online.YoutubeWebViewExtractor
import com.beatdrop.app.player.AudioFx

class BeatDropApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // WebView stream-extractor fallback needs an app context.
        YoutubeWebViewExtractor.init(this)
        // Restore the index of completed downloads.
        DownloadManager.init(this)
        // Load persisted equalizer settings (re-applied on each session attach).
        AudioFx.init(this)
        // Hydrate process-wide crossfade seconds for the CrossfadeEngine.
        com.beatdrop.app.data.local.SettingsStore(this)
    }
}
