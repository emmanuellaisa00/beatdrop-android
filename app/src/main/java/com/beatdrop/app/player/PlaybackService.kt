package com.beatdrop.app.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Real ExoPlayer running in a MediaSessionService:
 *  - background playback
 *  - system media notification + lock-screen transport controls
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var crossfade: CrossfadeEngine? = null

    override fun onCreate() {
        super.onCreate()
        // HTTP data source for online (googlevideo) streams. The default UA is an
        // Android-YouTube UA; per-track UAs are applied via MediaItem request headers.
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val httpFactory = OkHttpDataSource.Factory(okHttp)
            .setUserAgent("com.google.android.youtube/20.10.38 (Linux; U; Android 14; Pixel 8 Pro) gzip")
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Connection" to "keep-alive",
                )
            )
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Attach the real system audio effects to ExoPlayer's session.
        AudioFx.attach(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioFx.attach(audioSessionId)
            }
        })

        // Sleep timer pauses playback when the deadline passes.
        SleepTimer.bind(player)

        // Crossfade between tracks (volume-ramp; honors the setting, 0 = off).
        crossfade = CrossfadeEngine(player).also { it.start() }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        crossfade?.stop()
        crossfade = null
        SleepTimer.unbind()
        AudioFx.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
