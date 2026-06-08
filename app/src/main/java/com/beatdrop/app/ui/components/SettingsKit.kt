package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

@Composable
fun SettingsGroup(title: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = BeatType.SeeAll,
                color = BeatColors.TextMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BeatColors.Surface)
                .border(1.dp, BeatColors.GlassBorder, RoundedCornerShape(16.dp))
        ) { content() }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(BeatColors.SurfaceHover),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = BeatColors.TextSecondary, modifier = Modifier.size(19.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = BeatType.TrackTitle, color = BeatColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = BeatType.TrackSub, color = BeatColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun BeatSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = BeatColors.Accent,
            uncheckedThumbColor = BeatColors.Surface,
            uncheckedTrackColor = BeatColors.SurfaceHover,
            uncheckedBorderColor = BeatColors.GlassBorder,
        )
    )
}
