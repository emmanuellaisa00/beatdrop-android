package com.beatdrop.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.app.player.AudioFx
import com.beatdrop.app.ui.components.BeatSwitch
import com.beatdrop.app.ui.components.SettingsGroup
import com.beatdrop.app.ui.components.SettingsRow
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val fx by AudioFx.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        Column(
            Modifier.fillMaxSize().padding(top = 70.dp),
        ) {
            if (!fx.available) {
                Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Equalizer is unavailable on this device or until playback starts.",
                        style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
                    )
                }
            } else {
                // Enable toggle
                SettingsGroup {
                    SettingsRow(
                        Icons.Rounded.GraphicEq, "Equalizer",
                        subtitle = if (fx.enabled) "On" else "Off",
                        trailing = { BeatSwitch(fx.enabled, AudioFx::setEnabled) }
                    )
                }

                // Presets
                if (fx.presetNames.isNotEmpty()) {
                    Text(
                        "PRESETS", style = BeatType.SeeAll, color = BeatColors.TextMuted,
                        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fx.presetNames.forEachIndexed { i, name ->
                            val active = i == fx.currentPreset
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (active) BeatColors.Accent else Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
                                    .clickable(enabled = fx.enabled) { AudioFx.usePreset(i) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(name, style = BeatType.Pill, color = if (active) Color.White else BeatColors.TextSecondary)
                            }
                        }
                    }
                }

                // Bands — vertical sliders in a horizontal scroll
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().height(240.dp).horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fx.bands.forEach { band ->
                        BandColumn(band, enabled = fx.enabled)
                    }
                }

                // Bass boost
                SettingsGroup(title = "Bass boost") {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Slider(
                            value = fx.bassStrength.toFloat(),
                            onValueChange = { AudioFx.setBassStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = fx.enabled,
                            colors = sliderColors(),
                        )
                    }
                }
            }
        }
        StickyBackBar(title = "Equalizer", frosted = true, onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun BandColumn(band: AudioFx.Band, enabled: Boolean) {
    val db = band.levelMb / 100
    Column(
        Modifier.width(56.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${db}dB", style = BeatType.TabLabel, color = Color(0x99FFFFFF))
        // Vertical fader: a horizontal Slider rotated -90°, sized to the column height.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Slider(
                value = band.levelMb.toFloat(),
                onValueChange = { AudioFx.setBandLevel(band.index, it.toInt().toShort()) },
                valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                enabled = enabled,
                colors = sliderColors(),
                modifier = Modifier
                    .width(170.dp)
                    .rotate(-90f)
            )
        }
        Text(freqLabel(band.freqHz), style = BeatType.TabLabel, color = BeatColors.TextSecondary)
    }
}

private fun freqLabel(hz: Int): String = if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"

// (vertical fader is achieved by rotating the Slider -90°)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = BeatColors.Accent,
    inactiveTrackColor = Color(0x22FFFFFF),
    disabledThumbColor = Color(0x55FFFFFF),
    disabledActiveTrackColor = Color(0x22FFFFFF),
)


