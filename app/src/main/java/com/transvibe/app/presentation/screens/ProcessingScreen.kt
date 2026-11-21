package com.transvibe.app.presentation.screens

import android.app.Application
import android.os.SystemClock
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.JobState
import com.transvibe.app.domain.ProcessingProgress
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.theme.LocalStatusColors
import kotlinx.coroutines.delay

private enum class StageVisualState { COMPLETED, ACTIVE, PENDING, FAILED }

private data class PipelineStageItem(val state: JobState, val labelAr: String, val labelEn: String)

private val processingPipelineStages = listOf(
    PipelineStageItem(JobState.INSPECTING, "فحص الفيديو والوسائط", "Inspecting media"),
    PipelineStageItem(JobState.DECODING, "استخراج وتجهيز الصوت", "Decoding audio"),
    PipelineStageItem(JobState.RECOGNIZING, "التعرف على الكلام", "Recognizing speech"),
    PipelineStageItem(JobState.TRANSLATING, "ترجمة النصوص", "Translating subtitles"),
    PipelineStageItem(JobState.FINALIZING, "إنشاء وحفظ الترجمة", "Generating & saving subtitles")
)

@Composable
fun ProcessingScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBackHome: () -> Unit,
    onOpenResults: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val progress by viewModel.processingProgress.collectAsState()
    val jobId by viewModel.activeJobId.collectAsState()

    var clockMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    val progressTimestamp = remember(progress) { SystemClock.elapsedRealtime() }
    val p = progress
    val noTranscript = p?.state == JobState.FAILED && listOf(
        "AUDIO_NO_PCM", "VAD_NO_SPEECH", "ASR_NO_TRANSCRIPT"
    ).any { p.warningMessage?.contains(it) == true }

    val isActive = p != null && p.state !in setOf(
        JobState.COMPLETED,
        JobState.FAILED,
        JobState.CANCELLED,
        JobState.INTERRUPTED_RECOVERABLE
    )
    val elapsedSec = if (p != null) {
        (p.elapsedMs + if (isActive) (clockMs - progressTimestamp).coerceAtLeast(0L) else 0L) / 1000L
    } else 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "حالة المعالجة" else "Processing Status",
                subtitle = if (isArabic) "متابعة التفريغ الصوتي والترجمة خطوة بخطوة" else "Track transcription and translation step by step",
                icon = Icons.Rounded.AutoAwesome
            )
        }

        if (p != null && p.state != JobState.FAILED && p.state != JobState.CANCELLED) {
            item {
                OverallProgressRingCard(
                    progress = p,
                    elapsedSec = elapsedSec,
                    isArabic = isArabic
                )
            }
        }

        item {
            PipelineTimelineCard(progress = p, isArabic = isArabic)
        }

        if (p != null && (p.state == JobState.FAILED || p.state == JobState.INTERRUPTED_RECOVERABLE)) {
            item {
                ProcessingErrorBanner(
                    progress = p,
                    noTranscript = noTranscript,
                    isArabic = isArabic
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (jobId != null && !noTranscript && p?.state in setOf(
                        JobState.COMPLETED,
                        JobState.FAILED,
                        JobState.INTERRUPTED_RECOVERABLE
                    )
                ) {
                    PrimaryActionButton(
                        text = if (isArabic) "عرض النص المحفوظ وإعادة الترجمة" else "Open Saved Transcript & Results",
                        onClick = { onOpenResults(jobId!!) },
                        icon = Icons.Rounded.Subtitles,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isActive) {
                    FilledTonalButton(
                        onClick = onBackHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isArabic) "متابعة المعالجة في الخلفية" else "Continue in Background",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { viewModel.cancelProcessing(app) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء المعالجة" else "Cancel Processing",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    SecondaryActionButton(
                        text = if (isArabic) "العودة للرئيسية" else "Back to Home",
                        onClick = onBackHome,
                        icon = Icons.Rounded.Home,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallProgressRingCard(
    progress: ProcessingProgress,
    elapsedSec: Long,
    isArabic: Boolean
) {
    val ratio = when {
        progress.state == JobState.COMPLETED -> 1f
        progress.totalAudioMs > 0L -> (progress.processedAudioMs.toFloat() / progress.totalAudioMs.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    val animated by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(450),
        label = "overall-progress"
    )
    val scheme = MaterialTheme.colorScheme
    GlassCard(highlighted = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = scheme.primary,
                        trackColor = scheme.surfaceContainerHighest
                    )
                    Text(
                        text = "${(animated * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.primary
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (progress.state == JobState.TRANSLATING) {
                            if (isArabic) "ترجمة النصوص" else "Translating Subtitles"
                        } else {
                            if (isArabic) "تفريغ الصوت" else "Audio Transcription"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface
                    )
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = scheme.primary,
                        trackColor = scheme.surfaceContainerHighest
                    )
                    Text(
                        text = "${progress.processedAudioMs / 1000}s / ${progress.totalAudioMs / 1000}s  •  " +
                            (if (isArabic) "المنقضي: ${elapsedSec}ث" else "Elapsed: ${elapsedSec}s"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${progress.actualEngineId} • ${progress.actualModelId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (progress.state == JobState.TRANSLATING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.tertiaryContainer.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isArabic)
                                "مرحلة الترجمة مستقلة عن اكتمال الصوت، ولا تعني نسبة 100٪ اكتمال النتائج."
                            else
                                "Translation continues independently after audio reaches 100%; output is not yet complete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(100.dp)),
                            color = scheme.tertiary,
                            trackColor = scheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineTimelineCard(progress: ProcessingProgress?, isArabic: Boolean) {
    val activeIndex = when (progress?.state) {
        JobState.CREATED, JobState.QUEUED, JobState.INSPECTING -> 0
        JobState.DECODING -> 1
        JobState.RECOGNIZING -> 2
        JobState.TRANSLATING -> 3
        JobState.FINALIZING -> 4
        JobState.COMPLETED -> processingPipelineStages.size
        else -> 0
    }
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isArabic) "مراحل خط المعالجة" else "Processing Pipeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            processingPipelineStages.forEachIndexed { index, item ->
                val visual = when {
                    progress == null -> if (index == 0) StageVisualState.ACTIVE else StageVisualState.PENDING
                    progress.state == JobState.COMPLETED -> StageVisualState.COMPLETED
                    (progress.state == JobState.FAILED || progress.state == JobState.INTERRUPTED_RECOVERABLE) && index == activeIndex -> StageVisualState.FAILED
                    index < activeIndex -> StageVisualState.COMPLETED
                    index == activeIndex -> StageVisualState.ACTIVE
                    else -> StageVisualState.PENDING
                }
                PipelineStageRow(
                    label = if (isArabic) item.labelAr else item.labelEn,
                    visual = visual,
                    isArabic = isArabic,
                    showConnector = index < processingPipelineStages.lastIndex
                )
            }
        }
    }
}

@Composable
private fun PipelineStageRow(
    label: String,
    visual: StageVisualState,
    isArabic: Boolean,
    showConnector: Boolean
) {
    val semantic = LocalStatusColors.current
    val scheme = MaterialTheme.colorScheme
    val statusText = when (visual) {
        StageVisualState.COMPLETED -> if (isArabic) "مكتمل" else "Done"
        StageVisualState.ACTIVE -> if (isArabic) "قيد التنفيذ" else "Active"
        StageVisualState.PENDING -> if (isArabic) "قريباً" else "Pending"
        StageVisualState.FAILED -> if (isArabic) "فشل" else "Failed"
    }
    val tint = when (visual) {
        StageVisualState.COMPLETED -> semantic.ready
        StageVisualState.ACTIVE -> semantic.processing
        StageVisualState.PENDING -> scheme.outline
        StageVisualState.FAILED -> semantic.error
    }
    val containerTint = when (visual) {
        StageVisualState.COMPLETED -> semantic.readyContainer
        StageVisualState.ACTIVE -> semantic.processingContainer
        StageVisualState.PENDING -> scheme.surfaceContainerHigh
        StageVisualState.FAILED -> semantic.errorContainer
    }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse-$label")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(containerTint, CircleShape)
                    .then(
                        if (visual == StageVisualState.ACTIVE) {
                            Modifier.border(2.dp, tint.copy(alpha = pulseAlpha), CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (visual) {
                    StageVisualState.COMPLETED -> Icon(Icons.Rounded.CheckCircle, statusText, tint = tint, modifier = Modifier.size(20.dp))
                    StageVisualState.FAILED -> Icon(Icons.Rounded.Error, statusText, tint = tint, modifier = Modifier.size(20.dp))
                    StageVisualState.ACTIVE -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = tint,
                        trackColor = containerTint.copy(alpha = 0.5f)
                    )
                    StageVisualState.PENDING -> Icon(Icons.Rounded.RadioButtonUnchecked, statusText, tint = tint.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(
                            if (visual == StageVisualState.COMPLETED) tint.copy(alpha = 0.5f)
                            else tint.copy(alpha = 0.18f)
                        )
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (visual == StageVisualState.ACTIVE) FontWeight.Bold else FontWeight.SemiBold,
                color = if (visual == StageVisualState.PENDING) scheme.onSurfaceVariant else scheme.onSurface
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (visual == StageVisualState.ACTIVE || visual == StageVisualState.COMPLETED || visual == StageVisualState.FAILED) tint else scheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProcessingErrorBanner(
    progress: ProcessingProgress,
    noTranscript: Boolean,
    isArabic: Boolean
) {
    val semantic = LocalStatusColors.current
    val warning = if (noTranscript && isArabic) {
        when {
            progress.warningMessage?.contains("AUDIO_NO_PCM") == true -> "تعذر استخراج الصوت من الفيديو. تأكد من وجود مسار صوتي صالح."
            progress.warningMessage?.contains("VAD_NO_SPEECH") == true -> "لم يُكتشف كلام واضح في الصوت. تحقق من مستوى الصوت وأعد المحاولة."
            else -> "لم ينتج Whisper أي كلمات. تحقق من لغة الفيديو والنموذج، ثم أعد المحاولة."
        }
    } else progress.warningMessage ?: if (isArabic) "توقفت المعالجة؛ راجع حزم الترجمة والنماذج." else "Processing stopped; check translation packs and models."

    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(semantic.errorContainer.copy(alpha = 0.45f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(semantic.error.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = semantic.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = warning,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
