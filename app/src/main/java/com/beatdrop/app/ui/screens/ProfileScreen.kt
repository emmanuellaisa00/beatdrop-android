package com.beatdrop.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.ui.components.SettingsGroup
import com.beatdrop.app.ui.components.SettingsRow
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

@Composable
fun ProfileScreen(
    likedCount: Int,
    downloadCount: Int,
    sleepActiveLabel: String?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSleepTimer: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 70.dp, bottom = 200.dp)) {
            item { ProfileHeader() }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Rounded.Favorite, "Liked Songs", "$likedCount songs", onClick = onOpenLiked)
                    SettingsRow(Icons.Rounded.Download, "Downloads", "$downloadCount offline", onClick = onOpenDownloads)
                }
            }
            item {
                SettingsGroup(title = "Audio") {
                    SettingsRow(Icons.Rounded.Equalizer, "Equalizer", "Tune your sound", onClick = onOpenEqualizer)
                    SettingsRow(
                        Icons.Rounded.Bedtime, "Sleep timer",
                        subtitle = sleepActiveLabel ?: "Off",
                        onClick = onOpenSleepTimer
                    )
                }
            }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Rounded.Settings, "Settings", "Playback, quality, data", onClick = onOpenSettings)
                }
            }
        }
        StickyBackBar(title = "Profile", frosted = true, onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun ProfileHeader() {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFF375F), Color(0xFFB71F46)))),
            contentAlignment = Alignment.Center
        ) { Text("A", style = BeatType.LargeTitle.copy(fontSize = 40.sp), color = Color.White) }
        Text(
            "Alex",
            style = BeatType.LargeTitle.copy(fontSize = 26.sp, letterSpacing = (-0.03f).em),
            color = BeatColors.TextPrimary,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            "BeatDrop Premium",
            style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold),
            color = BeatColors.Accent,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
