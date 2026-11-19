package com.transvibe.app.presentation.screens

import android.app.Application
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
                Text("${p.processedAudioMs / 1000}s / ${p.totalAudioMs / 1000}s | Elapsed: ${p.elapsedMs / 1000}s")
            } else {
                Text(if (isArabic) "جاري التهيئة..." else "Initializing...")
            }
        }

        if (progress?.state == JobState.COMPLETED && jobId != null) {
            Button(onClick = { onOpenResults(jobId!!) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "عرض النتائج والمحرر" else "Open Results & Editor")
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
