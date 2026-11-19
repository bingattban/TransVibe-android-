package com.transvibe.app.presentation.screens

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.JobState
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard

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
    // Last progress update is a snapshot, not a continuously running timer.
    // Keep visible elapsed time ticking while ML Kit performs offline translation.
    var clockMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    val progressTimestamp = remember(progress) { SystemClock.elapsedRealtime() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (isArabic) "حالة المعالجة" else "Processing Status", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val p = progress
            if (p != null) {
                Text("Stage: ${p.stageLabel} (${p.state.name})")
                Text("Engine: ${p.actualEngineId} | Model: ${p.actualModelId}")
                val ratio = if (p.totalAudioMs > 0) (p.processedAudioMs.toFloat() / p.totalAudioMs).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
                val isActive = p.state != JobState.COMPLETED && p.state != JobState.FAILED &&
                    p.state != JobState.CANCELLED && p.state != JobState.INTERRUPTED_RECOVERABLE
                val elapsed = p.elapsedMs + if (isActive) (clockMs - progressTimestamp).coerceAtLeast(0L) else 0L
                Text("${p.processedAudioMs / 1000}s / ${p.totalAudioMs / 1000}s | Elapsed: ${elapsed / 1000}s")
                if (p.state == JobState.TRANSLATING) {
                    Text(if (isArabic) "مرحلة الترجمة مستقلة عن اكتمال الصوت" else "Translation continues after audio processing")
                }
                if (p.state == JobState.FAILED || p.state == JobState.INTERRUPTED_RECOVERABLE) {
                    Text(p.warningMessage ?: if (isArabic) "توقفت المعالجة؛ راجع حزم الترجمة" else "Processing stopped; check translation packs")
                }
            } else {
                Text(if (isArabic) "جاري التهيئة..." else "Initializing...")
            }
        }

        if (jobId != null && progress?.state in setOf(JobState.COMPLETED, JobState.FAILED, JobState.INTERRUPTED_RECOVERABLE)) {
            Button(onClick = { onOpenResults(jobId!!) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "عرض النص المحفوظ وإعادة الترجمة" else "Open saved transcript & retry translation")
            }
        } else {
            OutlinedButton(onClick = { viewModel.cancelProcessing(app) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "إلغاء المعالجة" else "Cancel")
            }
        }
        OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "العودة للرئيسية" else "Back to Home")
        }
    }
}
