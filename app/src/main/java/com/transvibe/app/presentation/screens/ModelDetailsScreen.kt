package com.transvibe.app.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun ModelDetailsScreen(
    modelId: String,
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val model = ModelCatalog.findById(modelId) ?: return
    val records by viewModel.modelRecords.collectAsState()
    val transferMsg by viewModel.modelTransferProgress.collectAsState()
    val rec = records[modelId]
    var confirmDelete by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importModelFromSaf(uri, model)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) viewModel.exportModelToSaf(model, uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isArabic) model.displayNameAr else model.displayNameEn, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("ID: ${model.modelId}")
            Text("File: ${model.physicalFileName}", color = TransVibeColors.ElectricCyan)
            Text("Engine: ${model.engineType.name}")
            Text("Languages: ${model.supportedLanguages.joinToString()}")
            Text("License: ${model.licenseName}")
            Text("Status: ${rec?.status ?: ModelStatus.NOT_INSTALLED}")
            Text("SHA-256: ${rec?.localSha256 ?: "Not computed yet"}")
        }

        transferMsg?.let { msg ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = TransVibeColors.WarmGold)
                OutlinedButton(onClick = { viewModel.cancelModelTransfer() }) {
                    Text(if (isArabic) "إلغاء النقل" else "Cancel Transfer")
                }
            }
        }

        Button(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isArabic) "استيراد عبر SAF (${model.physicalFileName})" else "Import via SAF (${model.physicalFileName})")
        }

        OutlinedButton(
            onClick = { showDownloadDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isArabic) "تنزيل عبر HTTPS" else "Download via HTTPS")
        }

        if (rec?.status == ModelStatus.READY) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.verifyModel(model) }, modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "تحقق" else "Verify")
                }
                OutlinedButton(
                    onClick = { exportLauncher.launch(model.physicalFileName) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isArabic) "تصدير" else "Export")
                }
            }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "حذف النموذج" else "Remove Model", color = TransVibeColors.CoralError)
            }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text(if (isArabic) "تنزيل النموذج (HTTPS)" else "HTTPS Model Download") },
            text = {
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (downloadUrl.isNotBlank()) {
                        viewModel.downloadModelFromHttps(downloadUrl.trim(), model)
                    }
                    showDownloadDialog = false
                }) { Text(if (isArabic) "تنزيل" else "Download") }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isArabic) "تأكيد الحذف" else "Confirm Delete") },
            text = { Text(if (isArabic) "لن تتأثر ملفات الفيديو أو المشاريع." else "Videos and project history will be preserved.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteModel(modelId)
                    confirmDelete = false
                }) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}
