package com.transvibe.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun ModelLibraryScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onModelClick: (String) -> Unit
) {
    val records by viewModel.modelRecords.collectAsState()
    val transferMsg by viewModel.modelTransferProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (isArabic) "مكتبة النماذج (7 معتمدة)" else "Model Library (Exact 7)",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        transferMsg?.let {
            Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ModelCatalog.ALL, key = { it.modelId }) { model ->
                val rec = records[model.modelId]
                val status = rec?.status ?: ModelStatus.NOT_INSTALLED
                val statusColor = when (status) {
                    ModelStatus.READY -> TransVibeColors.EmeraldSuccess
                    ModelStatus.CORRUPT, ModelStatus.FAILED -> androidx.compose.material3.MaterialTheme.colorScheme.error
                    else -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModelClick(model.modelId) }
                ) {
                    Text(if (isArabic) model.displayNameAr else model.displayNameEn)
                    Text(model.physicalFileName, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    Text("${model.engineType.name} • ${status.name}", color = statusColor)
                }
            }
        }
    }
}
