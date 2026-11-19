package com.transvibe.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.CheckCircle
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.StatusPill
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.BuildConfig
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.nativebridge.WhisperNativeBridge
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun HomeScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onNewVideo: () -> Unit,
    onModels: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBenchmark: () -> Unit,
    onOpenJob: (String) -> Unit
) {
    val records by viewModel.modelRecords.collectAsState()
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val readyCount = records.values.count { it.status == ModelStatus.READY }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeroHeader(
            title = if (isArabic) "ترجمة الفيديو محلياً" else "Local video subtitle translation",
            subtitle = if (isArabic) "خصوصيتك أولاً: تفريغ وترجمة على جهازك" else "Your video stays on your device",
            icon = Icons.Rounded.Subtitles,
            trailing = {
                OutlinedButton(onClick = onSettings) { Text(if (isArabic) "الإعدادات" else "Settings") }
            }
        )
        if (BuildConfig.DIAGNOSTIC_NATIVE_BUILD_SKIPPED && !WhisperNativeBridge.isAvailable()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isArabic)
                        "نسخة اختبار Code On The Go: لم تُكتشف مكتبة Whisper JNI أصلية متوافقة. ضع ملف libtransvibe_whisper.so المترجم لأندرويد ARM64 في app/libs/arm64-v8a/ قبل البناء. لا يثبت وجود الملف وحده صحة التعرف الصوتي."
                    else
                        "Code On The Go diagnostic build: compatible Whisper JNI is not linked. Place the Android ARM64 libtransvibe_whisper.so in app/libs/arm64-v8a/ before building. Presence alone does not prove real ASR works.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isArabic) "اختيار فيديو / ترجمة جديدة" else "Select Video / New Subtitles",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                text = if (isArabic) "اختيار فيديو" else "Pick Video",
                onClick = onNewVideo,
                icon = Icons.Rounded.VideoFile,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) {
            Text(if (isArabic) "النماذج المثبتة: $readyCount / 7"
                else "Models installed: $readyCount / 7",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium)
            StatusPill(
                label = if (readyCount > 0 && WhisperNativeBridge.isAvailable())
                    (if (isArabic) "جاهز" else "Ready")
                else (if (isArabic) "يحتاج إعداد" else "Setup needed"),
                color = if (readyCount > 0 && WhisperNativeBridge.isAvailable())
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                icon = Icons.Rounded.CheckCircle
            )
            Text(if (isArabic) "المشاريع المحفوظة: ${jobs.size}"
                else "Saved projects: ${jobs.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onModels, modifier = Modifier.weight(1f)) {
                Text(if (isArabic) "النماذج ($readyCount/7)" else "Models ($readyCount/7)")
            }
            OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) {
                Text(if (isArabic) "المشاريع (${jobs.size})" else "Projects (${jobs.size})")
            }
        }

        if (BuildConfig.ENABLE_BENCHMARK_PANEL) {
            OutlinedButton(onClick = onBenchmark, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "مختبر قياس الأداء (Debug/Preview)" else "Model Benchmark Lab (Debug/Preview)")
            }
        }

        Text(
            text = if (isArabic) "أحدث المشاريع" else "Recent Projects",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            jobs.take(5).forEach { job ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenJob(job.jobId) }
                ) {
                    Text(job.mediaTitle, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text("${job.actualModelId} • ${job.state}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
