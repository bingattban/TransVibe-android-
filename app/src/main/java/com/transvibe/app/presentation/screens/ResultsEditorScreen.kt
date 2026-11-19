package com.transvibe.app.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors
import com.transvibe.app.subtitles.SrtVttExporter

@Composable
fun ResultsEditorScreen(
    jobId: String,
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onPreviewPlayer: () -> Unit
) {
    LaunchedEffect(jobId) { viewModel.loadJobCues(jobId) }
    val cues by viewModel.activeCues.collectAsState()
    val jobMeta by viewModel.activeJobEntity.collectAsState()
    var bilingualExport by remember { mutableStateOf(false) }

    val srtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        if (uri != null) viewModel.exportSubtitles(uri, asVtt = false, bilingual = bilingualExport)
    }
    val vttLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vtt")) { uri ->
        if (uri != null) viewModel.exportSubtitles(uri, asVtt = true, bilingual = bilingualExport)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isArabic) "النتائج ومحرر الترجمة" else "Results & Subtitle Editor", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        jobMeta?.let { meta ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Engine: ${meta.actualEngineId} | Model: ${meta.actualModelId}", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                Text("Cues: ${cues.size} | SHA: ${meta.modelSha256?.take(12) ?: "local"}", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = bilingualExport, onCheckedChange = { bilingualExport = it })
            Text(if (isArabic) "تصدير ثنائي اللغة (عربي + أصلي)" else "Bilingual export")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onPreviewPlayer, modifier = Modifier.weight(1f)) {
                Text(if (isArabic) "معاينة" else "Preview")
            }
            OutlinedButton(onClick = { srtLauncher.launch("transvibe.srt") }, modifier = Modifier.weight(1f)) {
                Text("SRT")
            }
            OutlinedButton(onClick = { vttLauncher.launch("transvibe.vtt") }, modifier = Modifier.weight(1f)) {
                Text("VTT")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.retranslateActiveJob("ar") }, modifier = Modifier.weight(1f)) {
                Text(if (isArabic) "إعادة الترجمة" else "Re-translate")
            }
            OutlinedButton(
                onClick = { viewModel.undoCueEdit() },
                enabled = viewModel.canUndoCueEdit(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isArabic) "تراجع" else "Undo")
            }
            OutlinedButton(onClick = { viewModel.requestVideoBurnInCheck() }, modifier = Modifier.weight(1f)) {
                Text("MP4")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cues, key = { it.cueId }) { cue ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${SrtVttExporter.formatTimestamp(cue.startUs, ',')} -> ${SrtVttExporter.formatTimestamp(cue.endUs, ',')}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    if (cue.warnings.isNotEmpty()) {
                        Text(cue.warnings.joinToString(), color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary)
                    }
                    Text(cue.effectiveOriginal, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = cue.effectiveTranslation ?: "",
                        onValueChange = { viewModel.editCue(cue.cueId, null, it) },
                        label = { Text(if (isArabic) "الترجمة العربية" else "Arabic Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
