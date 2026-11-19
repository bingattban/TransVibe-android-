package com.transvibe.app.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.transvibe.app.BuildConfig
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun SettingsScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenTranslationPacks: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val cacheMb by viewModel.cacheSizeMb.collectAsState()
    var diagPreview by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize(app)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isArabic) "الإعدادات والخصوصية" else "Settings & Privacy", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "لغة الواجهة" else "UI Language")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.setUiLanguage("ar") }, modifier = Modifier.weight(1f)) {
                    Text("العربية")
                }
                OutlinedButton(onClick = { viewModel.setUiLanguage("en") }, modifier = Modifier.weight(1f)) {
                    Text("English")
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "حجم الذاكرة المؤقتة: $cacheMb MB" else "Temp/Staging Cache: $cacheMb MB")
            OutlinedButton(onClick = { viewModel.clearAppCache(app) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "مسح الملفات المؤقتة" else "Clear Temp Cache")
            }
        }

        OutlinedButton(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "إدارة النماذج السبعة" else "Manage 7 Models")
        }

        OutlinedButton(onClick = onOpenTranslationPacks, modifier = Modifier.fillMaxWidth()) {
            Text(if (isArabic) "حزم الترجمة المحلية" else "Translation Packs")
        }

        OutlinedButton(
            onClick = { diagPreview = viewModel.buildRedactedDiagnosticText() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isArabic) "إنشاء تقرير تشخيصي منقّح" else "Generate Redacted Diagnostics")
        }

        diagPreview?.let {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(it, color = TransVibeColors.TextSecondary)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("TransVibe", color = TransVibeColors.ElectricCyan)
            Text("Package: ${BuildConfig.APPLICATION_ID}")
            Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            if (BuildConfig.DIAGNOSTIC_NATIVE_ALLOWED) {
                Text("DEBUG / PREVIEW: وضع تشخيصي؛ بناء التطبيق لا يعني أن محركات التعرف الصوتي تعمل.")
            }
            Text(if (isArabic) "معالجة محلية 100% دون رفع ملفات." else "100% on-device offline processing.")
        }
    }
}
