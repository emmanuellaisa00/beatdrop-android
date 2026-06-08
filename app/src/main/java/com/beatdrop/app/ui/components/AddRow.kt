package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/**
 * `.add-row` — ported from beatdrop-premium.html.
 * row padding 16, r16, bg 0.06, gap 16; icon 46 gradient circle (accent by default);
 * title 15/800/-0.014em; desc 12/500/0.48; chevron 22/300 (›).
 */
@Composable
fun AddRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconBrush: Brush? = null,
    onClick: () -> Unit,
) {
    val accentBrush = Brush.linearGradient(listOf(BeatColors.Accent, BeatColors.AccentDeep))
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BeatColors.Surface)
            .border(1.dp, BeatColors.GlassBorder, RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(iconBrush ?: accentBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = BeatType.CardTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = BeatColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                description,
                style = BeatType.CardSub.copy(fontSize = 12.sp),
                color = BeatColors.TextTertiary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text("›", style = BeatType.LargeTitle.copy(fontSize = 22.sp, fontWeight = FontWeight.Light), color = BeatColors.TextFaint)
    }
}
