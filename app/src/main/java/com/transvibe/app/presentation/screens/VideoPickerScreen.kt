package com.transvibe.app.presentation.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.MetadataBadge
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import java.util.Locale

@Composable
fun VideoPickerScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onProceedToSetup: () -> Unit
) {
    val context = LocalContext.current.applicationContext as Application
    val inspected by viewModel.inspectedMedia.collectAsState()
    val selectedTrack by viewModel.selectedTrackIndex.collectAsState()
    val scheme = MaterialTheme.colorScheme

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.inspectSelectedVideo(context, uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "اختيار وفحص الفيديو" else "Select & Inspect Media",
                subtitle = if (isArabic) "اختر ملف فيديو أو صوت من جهازك عبر SAF" else "Choose local media from your device via SAF",
                icon = Icons.Rounded.VideoFile,
                trailing = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceContainer)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = scheme.onSurface
                        )
                    }
                }
            )
        }

        item {
            val borderColor = scheme.primary.copy(alpha = 0.55f)
            GlassCard(highlighted = inspected != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = borderColor,
                                cornerRadius = CornerRadius(22.dp.toPx()),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                                )
                            )
                        }
                        .clickable { pickerLauncher.launch(arrayOf("video/*", "audio/*")) }
                        .padding(12.dp)
                        .background(scheme.surface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(scheme.primary.copy(alpha = 0.25f), scheme.primaryContainer)
                                ),
                                CircleShape
                            )
                            .border(1.dp, scheme.primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = scheme.primary
                        )
                    }

                    if (inspected == null) {
                        Text(
                            text = if (isArabic) "اضغط لفتح مستعرض الملفات" else "Tap to Choose Local Video or Audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isArabic) "يدعم الفيديو والصوت المحلي بدون رفع للإنترنت" else "Supports local video & audio — stays offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val media = inspected!!
                        Text(
                            text = media.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        val totalSec = (media.durationUs / 1_000_000L).coerceAtLeast(0L)
                        MetadataBadge(
                            icon = Icons.Rounded.Schedule,
                            text = String.format(Locale.US, "%02d:%02d", totalSec / 60L, totalSec % 60L),
                            highlighted = true
                        )
                        SecondaryActionButton(
                            text = if (isArabic) "تغيير الملف" else "Change File",
                            onClick = { pickerLauncher.launch(arrayOf("video/*", "audio/*")) },
                            icon = Icons.Rounded.VideoFile,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        inspected?.let { media ->
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isArabic) "المسارات الصوتية (${media.audioTracks.size})" else "Audio Tracks (${media.audioTracks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                        }

                        media.audioTracks.forEach { track ->
                            val isSelected = selectedTrack == track.trackIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.selectAudioTrack(track.trackIndex) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectAudioTrack(track.trackIndex) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "#${track.trackIndex} • ${track.mimeType}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = "${track.sampleRateHz} Hz • ${track.channelCount}ch" +
                                                (track.language?.let { " • $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                PrimaryActionButton(
                    text = if (isArabic) "التالي: إعداد المعالجة" else "Next: Processing Setup",
                    onClick = onProceedToSetup,
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
