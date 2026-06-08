package com.beatdrop.app.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Real system audio effects (android.media.audiofx) attached to ExoPlayer's audio
 * session. The PlaybackService calls [attach] whenever the session id changes; the
 * Equalizer UI drives the setters here. Settings persist to SharedPreferences and
 * are re-applied on every (re)attach so they survive process death.
 */
object AudioFx {

    data class Band(val index: Int, val freqHz: Int, val minMb: Short, val maxMb: Short, val levelMb: Short)

    data class FxState(
        val available: Boolean = false,
        val enabled: Boolean = false,
        val bands: List<Band> = emptyList(),
        val presetNames: List<String> = emptyList(),
        val currentPreset: Int = -1,   // -1 = custom
        val bassStrength: Int = 0,      // 0..1000
    )

    private val _state = MutableStateFlow(FxState())
    val state: StateFlow<FxState> = _state

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var sessionId: Int = 0
    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("beatdrop_audiofx", Context.MODE_PRIVATE)
    }

    /** Called by PlaybackService with ExoPlayer's audio session id. */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (sessionId == audioSessionId && equalizer != null) return
        release()
        sessionId = audioSessionId
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val bb = BassBoost(0, audioSessionId).apply { setStrengthSupported() }
            equalizer = eq
            bassBoost = bb
            applyFromPrefs()
            publish()
        }.onFailure { _state.value = FxState(available = false) }
    }

    private fun BassBoost.setStrengthSupported() { if (strengthSupported) enabled = false }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
        sessionId = 0
    }

    // ── Controls ──

    fun setEnabled(on: Boolean) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = on
            bassBoost?.enabled = on && (prefs.getInt(KEY_BASS, 0) > 0)
        }
        prefs.edit().putBoolean(KEY_ON, on).apply()
        publish()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Short) {
        val eq = equalizer ?: return
        runCatching { eq.setBandLevel(bandIndex.toShort(), levelMb) }
        prefs.edit().putInt("$KEY_BAND$bandIndex", levelMb.toInt()).putInt(KEY_PRESET, -1).apply()
        publish()
    }

    fun usePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        runCatching { eq.usePreset(presetIndex.toShort()) }
        prefs.edit().putInt(KEY_PRESET, presetIndex).apply()
        // persist resulting band levels too
        runCatching {
            for (b in 0 until eq.numberOfBands) {
                prefs.edit().putInt("$KEY_BAND$b", eq.getBandLevel(b.toShort()).toInt()).apply()
            }
        }
        publish()
    }

    fun setBassStrength(strength: Int) {
        val bb = bassBoost ?: return
        val s = strength.coerceIn(0, 1000)
        runCatching {
            if (bb.strengthSupported) {
                bb.enabled = s > 0 && (equalizer?.enabled == true)
                if (s > 0) bb.setStrength(s.toShort())
            }
        }
        prefs.edit().putInt(KEY_BASS, s).apply()
        publish()
    }

    private fun applyFromPrefs() {
        val eq = equalizer ?: return
        val on = prefs.getBoolean(KEY_ON, false)
        val preset = prefs.getInt(KEY_PRESET, -1)
        runCatching {
            eq.enabled = on
            if (preset in 0 until eq.numberOfPresets) {
                eq.usePreset(preset.toShort())
            } else {
                for (b in 0 until eq.numberOfBands) {
                    val saved = prefs.getInt("$KEY_BAND$b", Int.MIN_VALUE)
                    if (saved != Int.MIN_VALUE) eq.setBandLevel(b.toShort(), saved.toShort())
                }
            }
        }
        val bass = prefs.getInt(KEY_BASS, 0)
        bassBoost?.let { bb ->
            runCatching {
                if (bb.strengthSupported && bass > 0) { bb.enabled = on; bb.setStrength(bass.toShort()) }
            }
        }
    }

    private fun publish() {
        val eq = equalizer
        if (eq == null) { _state.value = FxState(available = false); return }
        val bands = runCatching {
            val range = eq.bandLevelRange // [min,max] in millibels
            (0 until eq.numberOfBands).map { b ->
                Band(
                    index = b,
                    freqHz = eq.getCenterFreq(b.toShort()) / 1000,
                    minMb = range[0], maxMb = range[1],
                    levelMb = eq.getBandLevel(b.toShort()),
                )
            }
        }.getOrDefault(emptyList())
        val presets = runCatching {
            (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
        }.getOrDefault(emptyList())
        _state.value = FxState(
            available = true,
            enabled = runCatching { eq.enabled }.getOrDefault(false),
            bands = bands,
            presetNames = presets,
            currentPreset = prefs.getInt(KEY_PRESET, -1),
            bassStrength = prefs.getInt(KEY_BASS, 0),
        )
    }

    private const val KEY_ON = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BAND = "eq_band_"
    private const val KEY_BASS = "bass_strength"
}
