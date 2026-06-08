package com.beatdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.BrowsePalette

data class BrowseTile(
    val label: String,
    val palette: BrowsePalette,
    val icon: ImageVector? = null,
    val albumKey: String,
)

/**
 * Browse-tile grid — ported from `.browse-grid` / `.browse-tile`.
 * 2 columns, gap 12, tiles h100 r14, label 16/900/-0.022em, rotated 72dp deco glyph.
 */
@Composable
fun BrowseGrid(tiles: List<BrowseTile>, onClick: (BrowseTile) -> Unit) {
    androidx.compose.foundation.layout.Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTiles.forEach { tile ->
                    BrowseTileView(tile, Modifier.weight(1f)) { onClick(tile) }
                }
                if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BrowseTileView(tile: BrowseTile, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tile.palette.brush())
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        // ::after overlay (top-left highlight + bottom darken)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        0f to Color(0x2EFFFFFF),
                        0.58f to Color.Transparent,
                    )
                )
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color(0x38000000),
                    )
                )
        )
        // rotated deco glyph bottom-right
        if (tile.icon != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(72.dp)
                    .rotate(22f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x38000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tile.icon, null,
                    tint = Color(0xE0FFFFFF),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        // label top-left
        Text(
            tile.label,
            style = BeatType.SectionTitle.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.022f).em
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(14.dp)
        )
    }
}
