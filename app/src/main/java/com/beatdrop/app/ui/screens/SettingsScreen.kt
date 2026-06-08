package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.local.SettingsStore
import com.beatdrop.app.data.online.OnlinePlaybackDebugLog
import com.beatdrop.app.ui.components.BeatSwitch
import com.beatdrop.app.ui.components.SettingsGroup
import com.beatdrop.app.ui.components.SettingsRow
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    val store = SettingsStore(app)
}

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    BackHandler(onBack = onBack)
    val s by vm.store.state.collectAsStateWithLifecycle()
    val onlineLogs by OnlinePlaybackDebugLog.lines.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 70.dp, bottom = 200.dp)) {
            item {
                SettingsGroup(title = "Appearance") {
                    AppearanceRow(s.appearance, vm.store::setAppearance)
                }
            }
            item {
                SettingsGroup(title = "Playback") {
                    SettingsRow(Icons.Rounded.GraphicEq, "Gapless playback", "No silence between tracks",
                        trailing = { BeatSwitch(s.gaplessPlayback, vm.store::setGapless) })
                    SettingsRow(Icons.Rounded.VolumeUp, "Normalize volume", "Even loudness across songs",
                        trailing = { BeatSwitch(s.normalizeVolume, vm.store::setNormalize) })
                    CrossfadeRow(current = s.crossfadeSec, onChange = vm.store::setCrossfade)
                }
            }
            item {
                SettingsGroup(title = "Data") {
                    SettingsRow(Icons.Rounded.DataSaverOn, "Data saver", "Lower quality on mobile data",
                        trailing = { BeatSwitch(s.dataSaver, vm.store::setDataSaver) })
                }
            }
            item {
                SettingsGroup(title = "Quality") {
                    QualityRow("Streaming", s.streamQuality, vm.store::setStreamQuality)
                    QualityRow("Downloads", s.downloadQuality, vm.store::setDownloadQuality)
                }
            }
            item {
                SettingsGroup(title = "Diagnostics") {
                    SettingsRow(
                        Icons.Rounded.BugReport,
                        "Online playback logs",
                        if (onlineLogs.isEmpty()) "No logs yet" else "${onlineLogs.size} entries from last online playback",
                    )
                    SettingsRow(
                        Icons.Rounded.ContentCopy,
                        "Copy logs",
                        "Copy resolver and player events",
                        onClick = { clipboard.setText(AnnotatedString(OnlinePlaybackDebugLog.text().ifBlank { "No online playback logs yet." })) }
                    )
                    SettingsRow(
                        Icons.Rounded.DeleteSweep,
                        "Clear logs",
                        "Reset online playback diagnostics",
                        onClick = { OnlinePlaybackDebugLog.clear() }
                    )
                }
            }
        }
        StickyBackBar(title = "Settings", frosted = true, onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun AppearanceRow(
    current: SettingsStore.Appearance,
    onSelect: (SettingsStore.Appearance) -> Unit,
) {
    SettingsRow(
        Icons.Rounded.DarkMode,
        "Appearance",
        current.name.lowercase().replaceFirstChar { it.uppercase() },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsStore.Appearance.entries.forEach { mode ->
                    val active = mode == current
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) BeatColors.Accent else BeatColors.SurfaceHover)
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (mode) {
                                SettingsStore.Appearance.SYSTEM -> "Sys"
                                SettingsStore.Appearance.DARK -> "Dark"
                                SettingsStore.Appearance.LIGHT -> "Light"
                            },
                            style = BeatType.TabLabel,
                            color = if (active) Color.White else BeatColors.TextSecondary
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CrossfadeRow(current: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(BeatColors.SurfaceHover),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Tune, null, tint = BeatColors.TextSecondary, modifier = Modifier.size(19.dp)) }
            Column(Modifier.weight(1f)) {
                Text("Crossfade", style = BeatType.TrackTitle, color = BeatColors.TextPrimary)
                Text(
                    if (current <= 0) "Off" else "${current}s",
                    style = BeatType.TrackSub, color = BeatColors.TextSecondary
                )
            }
        }
        Slider(
            value = current.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..12f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = if (com.beatdrop.app.ui.theme.BeatThemeController.isLight) BeatColors.Accent else Color.White,
                activeTrackColor = BeatColors.Accent,
                inactiveTrackColor = BeatColors.SurfaceHover,
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun QualityRow(
    label: String,
    current: SettingsStore.Quality,
    onSelect: (SettingsStore.Quality) -> Unit,
) {
    SettingsRow(
        Icons.Rounded.HighQuality, label, current.name.lowercase().replaceFirstChar { it.uppercase() },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsStore.Quality.entries.forEach { q ->
                    val active = q == current
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) BeatColors.Accent else Color(0x14FFFFFF))
                            .clickable { onSelect(q) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            q.name.first().toString(),
                            style = BeatType.TabLabel,
                            color = if (active) Color.White else BeatColors.TextSecondary
                        )
                    }
                }
            }
        }
    )
}
