package com.transvibe.app.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transvibe.app.BuildConfig
import com.transvibe.app.domain.JobState
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.domain.ProcessingProgress
import com.transvibe.app.nativebridge.WhisperNativeBridge
import com.transvibe.app.persistence.JobEntity
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.MetadataBadge
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.components.StatusPill
import com.transvibe.app.presentation.theme.LocalStatusColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onNewVideo: () -> Unit,
    onSetup: () -> Unit,
    onModels: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBenchmark: () -> Unit,
    onOpenProcessing: () -> Unit,
    onOpenJob: (String) -> Unit
) {
    val records by viewModel.modelRecords.collectAsState()
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val inspectedMedia by viewModel.inspectedMedia.collectAsState()
    val progress by viewModel.processingProgress.collectAsState()
    val readyCount = records.values.count { it.status == ModelStatus.READY }
    val resourcesReady = readyCount > 0 && WhisperNativeBridge.isAvailable()
    val isBusy = progress?.state in setOf(
        JobState.CREATED,
        JobState.QUEUED,
        JobState.INSPECTING,
        JobState.DECODING,
        JobState.RECOGNIZING,
        JobState.TRANSLATING,
        JobState.FINALIZING
    )
    val scheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (isBusy) 112.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenHeroHeader(
                    title = if (isArabic) "ترجمة الفيديو محلياً" else "Local Video Subtitles",
                    subtitle = if (isArabic) "خصوصيتك أولاً: تفريغ وترجمة بالكامل على جهازك" else "Your video stays on your device — 100% offline",
                    icon = Icons.Rounded.Subtitles
                )
            }

            if (BuildConfig.DIAGNOSTIC_NATIVE_BUILD_SKIPPED && !WhisperNativeBridge.isAvailable()) {
                item {
                    GlassCard(highlighted = true) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = LocalStatusColors.current.warning,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isArabic)
                                    "نسخة اختبار Code On The Go: لم تُكتشف مكتبة Whisper JNI أصلية متوافقة. ضع ملف libtransvibe_whisper.so في app/libs/arm64-v8a/ قبل البناء."
                                else
                                    "Code On The Go diagnostic build: compatible Whisper JNI is not linked. Place ARM64 libtransvibe_whisper.so in app/libs/arm64-v8a/ before building.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                ReadinessHeroCard(
                    resourcesReady = resourcesReady,
                    readyCount = readyCount,
                    isArabic = isArabic,
                    onManageModels = onModels
                )
            }

            item {
                HomeVideoDropzone(
                    videoName = inspectedMedia?.displayName,
                    durationSec = inspectedMedia?.let { it.durationUs / 1_000_000L },
                    enabled = !isBusy,
                    isArabic = isArabic,
                    onPick = onNewVideo,
                    onProceedSetup = onSetup
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryActionButton(
                        text = if (isArabic) "النماذج ($readyCount/7)" else "Models ($readyCount/7)",
                        onClick = onModels,
                        icon = Icons.Rounded.Tune,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionButton(
                        text = if (isArabic) "السجل (${jobs.size})" else "History (${jobs.size})",
                        onClick = onHistory,
                        icon = Icons.Rounded.History,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (BuildConfig.ENABLE_BENCHMARK_PANEL) {
                item {
                    SecondaryActionButton(
                        text = if (isArabic) "مختبر قياس الأداء (Debug/Preview)" else "Model Benchmark Lab (Debug/Preview)",
                        onClick = onBenchmark,
                        icon = Icons.Rounded.Speed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (jobs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "أحدث المشاريع" else "Recent Projects",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "عرض الكل" else "See all",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onHistory)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                items(jobs.take(5), key = { it.jobId }) { job ->
                    HomeRecentJobCard(
                        job = job,
                        isArabic = isArabic,
                        onClick = { onOpenJob(job.jobId) }
                    )
                }
            }
        }

        if (isBusy && progress != null) {
            HomeActiveProcessingBanner(
                progress = progress!!,
                isArabic = isArabic,
                onClick = onOpenProcessing,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun ReadinessHeroCard(
    resourcesReady: Boolean,
    readyCount: Int,
    isArabic: Boolean,
    onManageModels: () -> Unit
) {
    val statusColors = LocalStatusColors.current
    val scheme = MaterialTheme.colorScheme
    val indicatorColor by animateColorAsState(
        targetValue = if (resourcesReady) statusColors.ready else statusColors.warning,
        animationSpec = tween(350),
        label = "readinessIndicator"
    )
    GlassCard(highlighted = !resourcesReady) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onManageModels)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(indicatorColor.copy(alpha = 0.16f))
                    .border(1.dp, indicatorColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (resourcesReady) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = indicatorColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (resourcesReady) {
                        if (isArabic) "جاهز للمعالجة المحلية" else "Ready for Offline Processing"
                    } else {
                        if (isArabic) "تحتاج الموارد إلى إعداد" else "Processing Resources Needed"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = if (resourcesReady) {
                        if (isArabic) "النماذج المثبتة: $readyCount من 7 — المحرك الأصلي متصل"
                        else "$readyCount of 7 models ready — native engine active"
                    } else {
                        if (isArabic) "ثبّت نموذج تعرف صوتي واحد على الأقل من مكتبة النماذج"
                        else "Install at least one ASR model from the Model Library"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                label = if (resourcesReady) (if (isArabic) "جاهز" else "Ready") else (if (isArabic) "إعداد" else "Setup"),
                color = indicatorColor,
                icon = if (resourcesReady) null else Icons.Rounded.Tune
            )
        }
    }
}

@Composable
private fun HomeVideoDropzone(
    videoName: String?,
    durationSec: Long?,
    enabled: Boolean,
    isArabic: Boolean,
    onPick: () -> Unit,
    onProceedSetup: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor = scheme.primary.copy(alpha = 0.55f)
    GlassCard(highlighted = videoName != null) {
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
                .clickable(enabled = enabled, onClick = onPick)
                .padding(12.dp)
                .background(
                    color = scheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                scheme.primary.copy(alpha = 0.25f),
                                scheme.primaryContainer
                            )
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

            if (videoName == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isArabic) "اختر فيديو لبدء الترجمة" else "Choose a Video to Subtitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isArabic) "MP4 • MKV • MOV • WEBM • صوت محلي" else "MP4 • MKV • MOV • WEBM • Local Audio",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = videoName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (durationSec != null && durationSec > 0L) {
                    val mins = durationSec / 60L
                    val secs = durationSec % 60L
                    MetadataBadge(
                        icon = Icons.Rounded.Schedule,
                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                        highlighted = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryActionButton(
                        text = if (isArabic) "تغيير" else "Change",
                        onClick = onPick,
                        enabled = enabled,
                        icon = Icons.Rounded.VideoFile,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryActionButton(
                        text = if (isArabic) "متابعة الإعداد" else "Setup",
                        onClick = onProceedSetup,
                        enabled = enabled,
                        icon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeRecentJobCard(
    job: JobEntity,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val statusColors = LocalStatusColors.current
    val pillColor = when (job.state) {
        JobState.COMPLETED.name -> statusColors.ready
        JobState.FAILED.name -> statusColors.error
        JobState.CANCELLED.name, JobState.INTERRUPTED_RECOVERABLE.name -> statusColors.warning
        else -> statusColors.processing
    }
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(scheme.primary.copy(alpha = 0.25f), scheme.primaryContainer)
                        ),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoFile,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = job.mediaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetadataBadge(Icons.Rounded.Memory, job.actualModelId)
                }
            }
            StatusPill(label = localizedJobState(job.state, isArabic), color = pillColor)
        }
    }
}

@Composable
private fun HomeActiveProcessingBanner(
    progress: ProcessingProgress,
    isArabic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = if (progress.totalAudioMs > 0L) {
        (progress.processedAudioMs.toFloat() / progress.totalAudioMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = ratio * 100f,
        animationSpec = tween(400),
        label = "homeProgressAnim"
    )
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.25.dp, scheme.primary.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(scheme.primary, CircleShape)
                    )
                    Text(
                        text = if (isArabic) "المعالجة قيد التنفيذ بالخلفية" else "Processing in Background",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface
                    )
                }
                Text(
                    text = "${animatedProgress.toInt()}%",
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            LinearProgressIndicator(
                progress = { (animatedProgress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(100.dp)),
                trackColor = scheme.surfaceContainerHighest,
                color = scheme.primary
            )
            Text(
                text = if (isArabic) "اضغط لفتح شاشة متابعة المعالجة" else "Tap to open processing status",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

internal fun localizedJobState(state: String, isArabic: Boolean): String = when (state) {
    JobState.COMPLETED.name -> if (isArabic) "مكتمل" else "Completed"
    JobState.FAILED.name -> if (isArabic) "فشل" else "Failed"
    JobState.CANCELLED.name -> if (isArabic) "ملغي" else "Cancelled"
    JobState.INTERRUPTED_RECOVERABLE.name -> if (isArabic) "متوقف" else "Interrupted"
    JobState.TRANSLATING.name -> if (isArabic) "ترجمة" else "Translating"
    JobState.RECOGNIZING.name -> if (isArabic) "تفريغ" else "Recognizing"
    JobState.DECODING.name -> if (isArabic) "فك الصوت" else "Decoding"
    else -> if (isArabic) "قيد العمل" else "Running"
}
