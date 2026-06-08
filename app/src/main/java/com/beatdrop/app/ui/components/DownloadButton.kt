package com.beatdrop.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.online.DownloadJob
import com.beatdrop.app.data.online.DownloadStatus
import com.beatdrop.app.ui.theme.BeatColors

/**
 * Per-row download affordance for online tracks:
 *   idle  → ↓
 *   queued → pulsing ↓
 *   downloading → progress ring with % (tap to cancel = ✕ on the ring)
 *   completed → ✓ (accent)
 *   failed → ✕ then back to ↓ on tap
 */
@Composable
fun DownloadButton(
    job: DownloadJob?,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = when {
        isDownloaded -> DownloadStatus.COMPLETED
        else -> job?.status ?: DownloadStatus.IDLE
    }
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        when (status) {
            DownloadStatus.IDLE, DownloadStatus.FAILED -> Icon(
                Icons.Rounded.Download, "Download",
                tint = if (status == DownloadStatus.FAILED) BeatColors.Accent else Color(0x99FFFFFF),
                modifier = Modifier.size(20.dp).clickable(onClick = onDownload)
            )
            DownloadStatus.QUEUED -> {
                val t = rememberInfiniteTransition(label = "queued")
                val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
                Icon(
                    Icons.Rounded.Download, "Queued",
                    tint = Color(0xCCFFFFFF),
                    modifier = Modifier.size(20.dp).alpha(a).clickable(onClick = onCancel)
                )
            }
            DownloadStatus.DOWNLOADING -> {
                val pct = (job?.percent ?: 0).coerceIn(0, 100)
                CircularProgressIndicator(
                    progress = { pct / 100f },
                    color = BeatColors.Accent,
                    trackColor = Color(0x22FFFFFF),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
                Icon(
                    Icons.Rounded.Close, "Cancel",
                    tint = Color(0xB3FFFFFF),
                    modifier = Modifier.size(12.dp).clickable(onClick = onCancel)
                )
            }
            DownloadStatus.COMPLETED -> Icon(
                Icons.Rounded.DownloadDone, "Downloaded",
                tint = BeatColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
