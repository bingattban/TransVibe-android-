package com.transvibe.app.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.MetadataBadge
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.components.StatusPill
import com.transvibe.app.presentation.theme.LocalStatusColors

@OptIn(ExperimentalLayoutApi::class)
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
    val status = rec?.status ?: ModelStatus.NOT_INSTALLED
    var confirmDelete by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    val statusColors = LocalStatusColors.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importModelFromSaf(uri, model)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) viewModel.exportModelToSaf(model, uri)
    }

    val statusColor = when (status) {
        ModelStatus.READY -> statusColors.ready
        ModelStatus.CORRUPT, ModelStatus.FAILED, ModelStatus.INCOMPATIBLE -> statusColors.error
        ModelStatus.TRANSFERRING, ModelStatus.VERIFYING, ModelStatus.INSTALLING -> statusColors.processing
        else -> statusColors.offline
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) model.displayNameAr else model.displayNameEn,
                subtitle = model.physicalFileName,
                icon = Icons.Rounded.Tune,
                trailing = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceContainer)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = scheme.onSurface
                        )
                    }
                }
            )
        }

        item {
            GlassCard(highlighted = status == ModelStatus.READY) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "بطاقة المواصفات والحالة" else "Specifications & Integrity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        StatusPill(
                            label = localizedModelStatus(status, isArabic),
                            color = statusColor,
                            icon = if (status == ModelStatus.READY) Icons.Rounded.CheckCircle else null
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataBadge(Icons.Rounded.Storage, "${model.minStorageMb} MB")
                        MetadataBadge(Icons.Rounded.Memory, "${model.minRamMb} MB RAM")
                        MetadataBadge(Icons.Rounded.VerifiedUser, model.licenseName, highlighted = true)
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))

                    DetailRow(if (isArabic) "المعرّف" else "ID", model.modelId)
                    DetailRow(if (isArabic) "الملف المطلوب" else "File", model.physicalFileName)
                    DetailRow(if (isArabic) "المحرك" else "Engine", model.engineType.name)
                    DetailRow(if (isArabic) "اللغات" else "Languages", model.supportedLanguages.joinToString(", "))
                    DetailRow("SHA-256", rec?.localSha256 ?: (if (isArabic) "لم يُحسب بعد" else "Not computed yet"))
                }
            }
        }

        transferMsg?.let { msg ->
            item {
                GlassCard(highlighted = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary
                        )
                        SecondaryActionButton(
                            text = if (isArabic) "إلغاء النقل" else "Cancel Transfer",
                            onClick = { viewModel.cancelModelTransfer() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryActionButton(
                    text = if (isArabic) "استيراد عبر SAF (${model.physicalFileName})" else "Import via SAF (${model.physicalFileName})",
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    icon = Icons.Rounded.FolderOpen,
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryActionButton(
                    text = if (isArabic) "تنزيل عبر HTTPS" else "Download via HTTPS",
                    onClick = { showDownloadDialog = true },
                    icon = Icons.Rounded.CloudDownload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (status == ModelStatus.READY) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryActionButton(
                            text = if (isArabic) "تحقق" else "Verify",
                            onClick = { viewModel.verifyModel(model) },
                            icon = Icons.Rounded.VerifiedUser,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryActionButton(
                            text = if (isArabic) "تصدير" else "Export",
                            onClick = { exportLauncher.launch(model.physicalFileName) },
                            icon = Icons.Rounded.Upload,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
                        border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isArabic) "حذف النموذج" else "Remove Model",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تنزيل النموذج (HTTPS)" else "HTTPS Model Download",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("https://...") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (downloadUrl.isNotBlank()) {
                        viewModel.downloadModelFromHttps(downloadUrl.trim(), model)
                    }
                    showDownloadDialog = false
                }) { Text(if (isArabic) "تنزيل" else "Download", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    text = if (isArabic) "تأكيد الحذف" else "Confirm Delete",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(if (isArabic) "لن تتأثر ملفات الفيديو أو المشاريع المحفوظة." else "Videos and project history will be preserved.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteModel(modelId)
                    confirmDelete = false
                }) {
                    Text(
                        text = if (isArabic) "حذف" else "Delete",
                        color = scheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
