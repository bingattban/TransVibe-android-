package com.transvibe.app.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.EngineType
import com.transvibe.app.domain.InstalledModelRecord
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.MetadataBadge
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.ScrollablePillTabs
import com.transvibe.app.presentation.components.SectionHeader
import com.transvibe.app.presentation.components.StatusPill
import com.transvibe.app.presentation.theme.LocalStatusColors
import java.util.Locale

@Composable
fun ModelLibraryScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onModelClick: (String) -> Unit
) {
    val records by viewModel.modelRecords.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val transferMsg by viewModel.modelTransferProgress.collectAsState()
    val packs by viewModel.translationPacks.collectAsState()
    var section by rememberSaveable { mutableIntStateOf(0) }

    val sections = listOf(
        "0" to "Whisper",
        "1" to "Moonshine",
        "2" to "SenseVoice",
        "3" to (if (isArabic) "حزم الترجمة" else "Translation Packs")
    )

    LaunchedEffect(Unit) {
        viewModel.refreshTranslationPacks()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeroHeader(
                title = if (isArabic) "إدارة النماذج" else "Model Manager",
                subtitle = if (isArabic) "النماذج السبعة المعتمدة وحزم الترجمة المحلية" else "Manage the 7 supported models & translation packs",
                icon = Icons.Rounded.Tune
            )

            ScrollablePillTabs(
                tabs = sections,
                selected = section.toString(),
                onSelected = { section = it.toIntOrNull() ?: 0 }
            )

            transferMsg?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.cancelModelTransfer() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = if (isArabic) "إلغاء" else "Cancel",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        when (section) {
            0 -> ModelCategoryList(
                title = if (isArabic) "نماذج Whisper" else "Whisper Models",
                subtitle = if (isArabic) "اضغط على أي نموذج للاستيراد عبر SAF أو التنزيل والتحقق" else "Tap any model to import via SAF, download, verify, or export",
                models = ModelCatalog.ALL.filter { it.engineType == EngineType.WHISPER_CPP },
                records = records,
                selectedModelId = selectedModel.modelId,
                isArabic = isArabic,
                onActivate = { viewModel.selectModel(it) },
                onOpenDetails = onModelClick
            )
            1 -> ModelCategoryList(
                title = if (isArabic) "نماذج Moonshine" else "Moonshine Models",
                subtitle = if (isArabic) "نموذج عربي سريع ومحلي عبر Sherpa-ONNX" else "Fast on-device Arabic ASR via Sherpa-ONNX",
                models = ModelCatalog.ALL.filter { it.engineType == EngineType.SHERPA_MOONSHINE_AR },
                records = records,
                selectedModelId = selectedModel.modelId,
                isArabic = isArabic,
                onActivate = { viewModel.selectModel(it) },
                onOpenDetails = onModelClick
            )
            2 -> ModelCategoryList(
                title = if (isArabic) "نماذج SenseVoice" else "SenseVoice Models",
                subtitle = if (isArabic) "محرك سريع متعدد اللغات عبر Sherpa-ONNX" else "Fast multilingual ASR via Sherpa-ONNX",
                models = ModelCatalog.ALL.filter { it.engineType == EngineType.SHERPA_SENSEVOICE },
                records = records,
                selectedModelId = selectedModel.modelId,
                isArabic = isArabic,
                onActivate = { viewModel.selectModel(it) },
                onOpenDetails = onModelClick
            )
            else -> TranslationPacksListContent(
                packs = packs,
                isArabic = isArabic,
                onRetryPending = { viewModel.retryPendingTranslationPacks() },
                onDownload = { viewModel.downloadTranslationPack(it) },
                onDelete = { viewModel.deleteTranslationPack(it) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCategoryList(
    title: String,
    subtitle: String,
    models: List<ModelDefinition>,
    records: Map<String, InstalledModelRecord>,
    selectedModelId: String,
    isArabic: Boolean,
    onActivate: (ModelDefinition) -> Unit,
    onOpenDetails: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title, subtitle) }
        items(models, key = { it.modelId }) { model ->
            val rec = records[model.modelId]
            val status = rec?.status ?: ModelStatus.NOT_INSTALLED
            val isReady = status == ModelStatus.READY
            val isSelected = selectedModelId == model.modelId
            val isRecommended = model.modelId == ModelCatalog.WHISPER_TINY_ID || model.modelId == ModelCatalog.WHISPER_BASE_ID
            val scheme = MaterialTheme.colorScheme
            val statusColors = LocalStatusColors.current
            val pillColor = when (status) {
                ModelStatus.READY -> statusColors.ready
                ModelStatus.CORRUPT, ModelStatus.FAILED, ModelStatus.INCOMPATIBLE -> statusColors.error
                ModelStatus.TRANSFERRING, ModelStatus.VERIFYING, ModelStatus.INSTALLING, ModelStatus.STAGING -> statusColors.processing
                else -> statusColors.offline
            }

            GlassCard(highlighted = isSelected && isReady) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetails(model.modelId) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isArabic) model.displayNameAr else model.displayNameEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = scheme.onSurface
                            )
                            Text(
                                text = model.physicalFileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isRecommended) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = scheme.tertiaryContainer.copy(alpha = 0.75f),
                                    border = BorderStroke(1.dp, scheme.tertiary.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = if (isArabic) "موصى به" else "Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isReady) {
                            FilledTonalButton(
                                onClick = { onActivate(model) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) scheme.primary else scheme.primaryContainer,
                                    contentColor = if (isSelected) scheme.onPrimary else scheme.onPrimaryContainer
                                )
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (isSelected) {
                                        if (isArabic) "محدد" else "Active"
                                    } else {
                                        if (isArabic) "تفعيل" else "Select"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = if (isSelected) 6.dp else 0.dp)
                                )
                            }
                        } else {
                            StatusPill(
                                label = localizedModelStatus(status, isArabic),
                                color = pillColor
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetadataBadge(Icons.Rounded.Storage, "${model.minStorageMb} MB")
                        MetadataBadge(Icons.Rounded.Memory, "${model.minRamMb} MB RAM")
                        if (model.engineType != EngineType.WHISPER_CPP) {
                            MetadataBadge(
                                Icons.Rounded.Tune,
                                model.supportedLanguages.joinToString(" / ") { it.uppercase(Locale.ROOT) },
                                highlighted = true
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "اضغط لإدارة النموذج (استيراد، تنزيل، تحقق، تصدير، حذف)" else "Tap to manage model (import, download, verify, export, delete)",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

internal fun localizedModelStatus(status: ModelStatus, isArabic: Boolean): String = when (status) {
    ModelStatus.READY -> if (isArabic) "مثبت وجاهز" else "Ready"
    ModelStatus.NOT_INSTALLED -> if (isArabic) "غير مثبت" else "Not installed"
    ModelStatus.TRANSFERRING -> if (isArabic) "جاري النقل" else "Transferring"
    ModelStatus.STAGING -> if (isArabic) "تجهيز" else "Staging"
    ModelStatus.VERIFYING -> if (isArabic) "تحقق" else "Verifying"
    ModelStatus.INSTALLING -> if (isArabic) "تثبيت" else "Installing"
    ModelStatus.CORRUPT -> if (isArabic) "تالف" else "Corrupt"
    ModelStatus.FAILED -> if (isArabic) "فشل" else "Failed"
    ModelStatus.INCOMPATIBLE -> if (isArabic) "غير متوافق" else "Incompatible"
    ModelStatus.REPAIRABLE -> if (isArabic) "قابل للإصلاح" else "Repairable"
}
