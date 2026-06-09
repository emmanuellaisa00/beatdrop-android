package com.beatdrop.app.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** App playback/display preferences (SharedPreferences-backed, observable). */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("beatdrop_settings", Context.MODE_PRIVATE)

    data class Settings(
        val dataSaver: Boolean = false,
        val gaplessPlayback: Boolean = true,
        val normalizeVolume: Boolean = false,
        val crossfadeSec: Int = 0,            // 0 = off, up to 12s
        val streamQuality: Quality = Quality.AUTO,
        val downloadQuality: Quality = Quality.HIGH,
    )

    enum class Quality { LOW, NORMAL, HIGH, AUTO }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state

    fun setDataSaver(v: Boolean) = put { it.copy(dataSaver = v) }.also { prefs.edit().putBoolean(DATA_SAVER, v).apply() }
    fun setGapless(v: Boolean) = put { it.copy(gaplessPlayback = v) }.also { prefs.edit().putBoolean(GAPLESS, v).apply() }
    fun setNormalize(v: Boolean) = put { it.copy(normalizeVolume = v) }.also { prefs.edit().putBoolean(NORMALIZE, v).apply() }
    fun setCrossfade(sec: Int) = put { it.copy(crossfadeSec = sec) }.also {
        prefs.edit().putInt(CROSSFADE, sec).apply()
        CrossfadeSettings.seconds = sec
    }
    fun setStreamQuality(q: Quality) = put { it.copy(streamQuality = q) }.also { prefs.edit().putString(STREAM_Q, q.name).apply() }
    fun setDownloadQuality(q: Quality) = put { it.copy(downloadQuality = q) }.also { prefs.edit().putString(DL_Q, q.name).apply() }
    private inline fun put(update: (Settings) -> Settings) { _state.value = update(_state.value) }

    private fun load() = Settings(
        dataSaver = prefs.getBoolean(DATA_SAVER, false),
        gaplessPlayback = prefs.getBoolean(GAPLESS, true),
        normalizeVolume = prefs.getBoolean(NORMALIZE, false),
        crossfadeSec = prefs.getInt(CROSSFADE, 0),
        streamQuality = runCatching { Quality.valueOf(prefs.getString(STREAM_Q, "AUTO")!!) }.getOrDefault(Quality.AUTO),
        downloadQuality = runCatching { Quality.valueOf(prefs.getString(DL_Q, "HIGH")!!) }.getOrDefault(Quality.HIGH),
    ).also { CrossfadeSettings.seconds = it.crossfadeSec }

    companion object {
        private const val DATA_SAVER = "data_saver"
        private const val GAPLESS = "gapless"
        private const val NORMALIZE = "normalize"
        private const val CROSSFADE = "crossfade_sec"
        private const val STREAM_Q = "stream_quality"
        private const val DL_Q = "download_quality"
    }
}

/** Shared, process-wide crossfade seconds so the CrossfadeEngine (in the service) can read it. */
object CrossfadeSettings {
    @Volatile var seconds: Int = 0
}
