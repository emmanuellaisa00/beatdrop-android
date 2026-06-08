package com.beatdrop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatThemeController
import com.beatdrop.app.ui.theme.BeatType

/** A tappable icon used inside the compact header (.compact .icons svg). */
data class HeaderIcon(val icon: ImageVector, val desc: String, val onClick: () -> Unit)

/**
 * Frosted compact header — ported from `.compact` in beatdrop-premium.html.
 * Pinned to the top; fades/slides in (opacity 0→1, translateY -6px→0) once the
 * underlying list is scrolled past ~120px, exactly like the HTML scroll listener.
 *
 * height 98 (54 status + 12 bottom), bg gradient rgba(8,6,10,0.88)→transparent,
 * title 17/800/-0.016em, icons 22px / gap 22.
 */
@Composable
fun CompactHeader(
    title: String,
    listState: LazyListState,
    icons: List<HeaderIcon> = emptyList(),
    thresholdDp: Int = 120,
    modifier: Modifier = Modifier,
) {
    val thresholdPx = with(LocalDensity.current) { thresholdDp.dp.toPx() }
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > thresholdPx
        }
    }

    AnimatedVisibility(
        visible = scrolled,
        enter = fadeIn() + slideInVertically { -it / 4 },
        exit = fadeOut() + slideOutVertically { -it / 4 },
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to if (BeatThemeController.isLight) Color(0xF7FFFFFF) else Color(0xE008060A),
                        0.65f to if (BeatThemeController.isLight) Color(0xF7FFFFFF) else Color(0xE008060A),
                        1f to if (BeatThemeController.isLight) Color(0x00FFFFFF) else Color(0x0008060A),
                    )
                )
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = BeatType.TopBarTitle, color = BeatColors.TextPrimary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icons.forEach { hi ->
                        Icon(
                            hi.icon, hi.desc,
                            tint = BeatColors.TextPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = hi.onClick
                                )
                        )
                    }
                }
            }
        }
    }
}
