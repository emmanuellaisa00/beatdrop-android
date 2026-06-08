package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType

/**
 * Shared hero header: avatar + greeting + action icons, then a big two-tone large title.
 * `titlePlain` + `titleAccent` reproduces "Good <vibes>" with the gradient accent word.
 */
@Composable
fun Hero(
    greetingPlain: String,
    greetingBold: String? = null,
    titlePlain: String,
    titleAccent: String,
    showActions: Boolean = true,
    onSearch: () -> Unit = {},
    onAdd: () -> Unit = {},
) {
    val onAvatar = LocalOpenProfile.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFF375F), Color(0xFFB71F46))))
                    .clickableNoRipple(onAvatar),
                contentAlignment = Alignment.Center
            ) {
                Text("A", style = BeatType.CardTitle, color = Color.White)
            }
            Box(Modifier.padding(start = 12.dp)) {
                Text(
                    buildAnnotatedString {
                        append(greetingPlain)
                        if (greetingBold != null) {
                            withStyle(SpanStyle(color = BeatColors.TextPrimary)) {
                                append(" $greetingBold")
                            }
                        }
                    },
                    style = BeatType.Greet,
                    color = BeatColors.TextSecondary
                )
            }
            if (showActions) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search, "Search",
                        tint = BeatColors.TextSecondary,
                        modifier = Modifier.size(21.dp).clip(CircleShape).clickableNoRipple(onSearch)
                    )
                    Icon(
                        Icons.Rounded.Add, "Add",
                        tint = BeatColors.TextSecondary,
                        modifier = Modifier.size(21.dp).clip(CircleShape).clickableNoRipple(onAdd)
                    )
                }
            }
        }
        Text(
            buildLargeTitle(titlePlain, titleAccent),
            style = BeatType.LargeTitle
        )
    }
}

private fun buildLargeTitle(plain: String, accent: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = BeatColors.TextPrimary)) { append("$plain ") }
    // approximate the clipped gradient text with the accent color
    withStyle(SpanStyle(color = BeatColors.Accent)) { append(accent) }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
    ) { onClick() }
