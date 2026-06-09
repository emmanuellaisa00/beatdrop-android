package com.beatdrop.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatdrop.app.ui.theme.BeatColors
import androidx.compose.ui.unit.em
import com.beatdrop.app.ui.theme.BeatType

/**
 * Sticky-back bar — ported from `.sticky-back` / `.sticky-back.frosted`.
 * Always-visible back + more buttons; the centered title and frosted background
 * fade in once `frosted` becomes true (scrolled past ~120px), mirroring the HTML.
 */
@Composable
fun StickyBackBar(
    title: String,
    frosted: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit = {},
    moreIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.MoreHoriz,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        if (frosted) Color(0xD9101B2A) else Color.Transparent,
        label = "bgFrost"
    )
    val titleAlpha by animateFloatAsState(if (frosted) 1f else 0f, label = "titleAlpha")

    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to bg,
                    0.6f to bg,
                    1f to Color.Transparent,
                )
            )
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            // back (left)
            CircleIcon(
                Icons.AutoMirrored.Rounded.ArrowBackIos, "Back",
                modifier = Modifier.align(Alignment.CenterStart), onClick = onBack
            )
            // centered title (fades in)
            Text(
                title,
                style = BeatType.TopBarTitle.copy(letterSpacing = (-0.016f).em),
                color = BeatColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
                    .alpha(titleAlpha)
            )
            // more (right)
            CircleIcon(
                moreIcon, "More",
                modifier = Modifier.align(Alignment.CenterEnd), onClick = onMore
            )
        }
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0x99101B2A))
            .border(1.dp, BeatColors.GlassBorder, CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = BeatColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}
