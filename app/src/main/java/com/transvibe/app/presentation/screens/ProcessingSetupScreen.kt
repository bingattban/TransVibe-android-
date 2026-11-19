package com.transvibe.app.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import com.transvibe.app.presentation.components.ScreenHeroHeader

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun ProcessingSetupScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenTranslationPacks: () -> Unit,
    onStartProcessing: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val selectedModel by viewModel.selectedModel.collectAsState()
    val records by viewModel.modelRecords.collectAsState()
    var sourceLang by remember { mutableStateOf("auto") }
    var targetLang by remember { mutableStateOf("ar") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeroHeader(
            title = if (isArabic) "إعداد التفريغ والترجمة" else "Processing Setup",
            subtitle = if (isArabic) "تحديد النموذج واللغة قبل المعالجة" else "Select model and language",
            icon = Icons.Rounded.Tune,
            trailing = {
                OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
            }
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "اختر النموذج:" else "Select Model:")
            ModelCatalog.ALL.forEach { m ->
                val ready = records[m.modelId]?.status == ModelStatus.READY
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedModel.modelId == m.modelId, onClick = { viewModel.selectModel(m) })
                    Text("${m.physicalFileName} ${if (ready) "✓" else "(Missing)"}")
                }
            }
            if (records[selectedModel.modelId]?.status != ModelStatus.READY) {
                OutlinedButton(onClick = onOpenModels) {
                    Text(if (isArabic) "تثبيت النموذج من المكتبة" else "Install Model in Library", color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { sourceLang = "auto" }, modifier = Modifier.weight(1f)) { Text("Src: Auto") }
            OutlinedButton(onClick = { sourceLang = "en" }, modifier = Modifier.weight(1f)) { Text("Src: EN") }
            OutlinedButton(onClick = { sourceLang = "ar" }, modifier = Modifier.weight(1f)) { Text("Src: AR") }
        }

        OutlinedButton(onClick = onOpenTranslationPacks, modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "إدارة حزم الترجمة (ML Kit)" else "Manage Translation Packs", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                if (viewModel.startProcessing(app, sourceLang, targetLang)) {
                    onStartProcessing()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isArabic) "بدء المعالجة" else "Start Processing")
        }
    }
}
