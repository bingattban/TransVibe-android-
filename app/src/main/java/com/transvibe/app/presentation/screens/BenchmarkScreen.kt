package com.transvibe.app.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun BenchmarkScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val summaries by viewModel.benchmarkSummaries.collectAsState()
    val freezeRecords by viewModel.freezeRecords.collectAsState()
    val tinyFreeze = freezeRecords[ModelCatalog.WHISPER_TINY_ID]
    var corpusUri by remember { mutableStateOf<Uri?>(null) }

    val corpusPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) corpusUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isArabic) "دراسة أداء Whisper Tiny" else "Tiny Benchmark Study", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Tiny Freeze Status: ${tinyFreeze?.status ?: "UNDER_DEVELOPMENT"}", color = TransVibeColors.WarmGold)
            Text("Owner Approved: ${tinyFreeze?.approvedByOwner ?: false}")
            Text("Reference Clip: ${corpusUri?.lastPathSegment ?: "None selected"}")
        }

        OutlinedButton(onClick = { corpusPicker.launch(arrayOf("audio/*", "video/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "اختيار مقطع صوتي مرجعي" else "Select Reference Corpus Clip")
        }

        Button(onClick = { viewModel.runTinyBenchmarkStudy(corpusUri) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "تشغيل اختبار استدلال حقيقي (2/3/4 خيوط)" else "Run Real Inference Study (2/3/4 Threads)")
        }

        summaries.forEach { s ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Threads: ${s.threads} (${s.runCount} runs)", color = TransVibeColors.ElectricCyan)
                Text("Median Inference: ${s.medianInferenceMs} ms | p90: ${s.p90InferenceMs} ms")
                Text("Peak PSS: ${s.maxPeakPssKb} KB")
            }
        }

        if (summaries.isNotEmpty() && tinyFreeze?.approvedByOwner != true) {
            OutlinedButton(
                onClick = { viewModel.approveModelFreeze(ModelCatalog.WHISPER_TINY_ID) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isArabic) "اعتماد وتجميد Whisper Tiny (موافقة المالك)" else "Owner Approve & Freeze Tiny")
            }
        }
    }
}
