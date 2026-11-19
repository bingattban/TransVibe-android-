package com.transvibe.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors
import com.transvibe.app.translation.PackTransferState

@Composable
fun TranslationPacksScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val packs by viewModel.translationPacks.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshTranslationPacks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (isArabic) "حزم الترجمة (ML Kit)" else "Translation Packs",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        OutlinedButton(
            onClick = { viewModel.retryPendingTranslationPacks() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isArabic) "إعادة محاولة التنزيلات المعلقة" else "Retry Pending Downloads")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packs, key = { it.code }) { pack ->
                val stateColor = when (pack.state) {
                    PackTransferState.INSTALLED -> TransVibeColors.EmeraldSuccess
                    PackTransferState.FAILED -> androidx.compose.material3.MaterialTheme.colorScheme.error
                    else -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${if (isArabic) pack.nameAr else pack.nameEn} (${pack.code})")
                        if (pack.isCore) {
                            Text(
                                if (isArabic) "أساسية" else "CORE",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text("Status: ${pack.state.name} | Offline: ${pack.offlineReady}", color = stateColor)
                    Text(pack.sizeLabel, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    pack.lastError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!pack.offlineReady) {
                            Button(onClick = { viewModel.downloadTranslationPack(pack.code) }) {
                                Text(if (isArabic) "تنزيل" else "Download")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.downloadTranslationPack(pack.code) }) {
                                Text(if (isArabic) "فحص التحديث" else "Check Update")
                            }
                            OutlinedButton(onClick = { viewModel.deleteTranslationPack(pack.code) }) {
                                Text(if (isArabic) "حذف" else "Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
