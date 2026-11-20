package com.transvibe.app.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import com.transvibe.app.presentation.components.ScreenHeroHeader

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors
import com.transvibe.app.subtitles.SrtVttExporter
import kotlinx.coroutines.delay

@Composable
fun PlayerPreviewScreen(
    jobId: String,
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val jobMeta by viewModel.activeJobEntity.collectAsState()
    val cues by viewModel.activeCues.collectAsState()
    val mediaUriString = previewMediaUriForJob(jobId, jobMeta?.jobId, jobMeta?.mediaUriString)
    val currentCues = if (mediaUriString != null) cues else emptyList()
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var fontSizeSp by remember { mutableFloatStateOf(16f) }

    // The selected video may not be the video's URI for this saved job.
    LaunchedEffect(jobId) { viewModel.loadJobCues(jobId) }

    val player = remember(context) { ExoPlayer.Builder(context).build() }
    // The persisted job may arrive after composition. Rebind media on change.
    LaunchedEffect(player, mediaUriString) {
        player.stop()
        player.clearMediaItems()
        currentPosMs = 0L
        if (mediaUriString != null) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUriString)))
            player.prepare()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosMs = player.currentPosition
            delay(100L)
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    val activeCue = currentCues.firstOrNull {
        val posUs = currentPosMs * 1000L
        posUs in it.startUs..it.endUs
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenHeroHeader(
            title = if (isArabic) "معاينة الفيديو والترجمة" else "Video Preview",
            subtitle = if (isArabic) "عرض الفيديو مع الترجمة النصية" else "Video with timed subtitles",
            icon = Icons.Rounded.PlayArrow,
            trailing = {
                OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
            }
        )

        Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
                modifier = Modifier.fillMaxSize()
            )
            activeCue?.let { cue ->
                Text(
                    text = cue.effectiveTranslation ?: cue.effectiveOriginal,
                    color = Color.White,
                    fontSize = fontSizeSp.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(Color(0xCC090D16))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isArabic) "حجم الخط: ${fontSizeSp.toInt()}" else "Font: ${fontSizeSp.toInt()}sp")
            Slider(
                value = fontSizeSp,
                onValueChange = { fontSizeSp = it },
                valueRange = 12f..26f,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = if (isArabic) "اضغط على أي سطر للانتقال إليه:" else "Tap a cue to seek:",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(currentCues, key = { it.cueId }) { cue ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { player.seekTo(cue.startUs / 1000L) }
                ) {
                    Text(SrtVttExporter.formatTimestamp(cue.startUs, ','), color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    Text(cue.effectiveTranslation ?: cue.effectiveOriginal)
                }
            }
        }
    }
}
