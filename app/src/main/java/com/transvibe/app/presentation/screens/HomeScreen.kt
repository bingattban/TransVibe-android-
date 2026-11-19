package com.transvibe.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TransVibe", color = TransVibeColors.ElectricCyan, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onSettings) {
                Text(if (isArabic) "الإعدادات" else "Settings")
            }
        }

        if (BuildConfig.DIAGNOSTIC_NATIVE_BUILD_SKIPPED && !WhisperNativeBridge.isAvailable()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isArabic)
                        "نسخة اختبار Code On The Go: لم تُكتشف مكتبة Whisper JNI أصلية متوافقة. ضع ملف libtransvibe_whisper.so المترجم لأندرويد ARM64 في app/libs/arm64-v8a/ قبل البناء. لا يثبت وجود الملف وحده صحة التعرف الصوتي."
                    else
                        "Code On The Go diagnostic build: compatible Whisper JNI is not linked. Place the Android ARM64 libtransvibe_whisper.so in app/libs/arm64-v8a/ before building. Presence alone does not prove real ASR works.",
                    color = TransVibeColors.TextSecondary
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isArabic) "اختيار فيديو / ترجمة جديدة" else "Select Video / New Subtitles",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNewVideo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TransVibeColors.ElectricCyan)
            ) {
                Text(if (isArabic) "اختيار فيديو" else "Pick Video", color = TransVibeColors.MidnightNavy)
            }
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs.take(5), key = { it.jobId }) { job ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenJob(job.jobId) }
                ) {
                    Text(job.mediaTitle, color = TransVibeColors.TextPrimary)
                    Text("${job.actualModelId} • ${job.state}", color = TransVibeColors.TextSecondary)
                }
            }
        }
    }
}
