package com.transvibe.app.presentation.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.DropdownChoice
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.components.SegmentedPillSelector
import com.transvibe.app.presentation.theme.LocalStatusColors

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
    val scheme = MaterialTheme.colorScheme
    val selectedReady = records[selectedModel.modelId]?.status == ModelStatus.READY

    val sourceOptions = listOf(
        "auto" to (if (isArabic) "تلقائي (كشف لغة الفيديو)" else "Auto-detect"),
        "en" to (if (isArabic) "الإنجليزية (EN)" else "English (EN)"),
        "ar" to (if (isArabic) "العربية (AR)" else "Arabic (AR)"),
        "fr" to (if (isArabic) "الفرنسية (FR)" else "French (FR)"),
        "de" to (if (isArabic) "الألمانية (DE)" else "German (DE)"),
        "es" to (if (isArabic) "الإسبانية (ES)" else "Spanish (ES)"),
        "tr" to (if (isArabic) "التركية (TR)" else "Turkish (TR)"),
        "ur" to (if (isArabic) "الأردية (UR)" else "Urdu (UR)"),
        "ja" to (if (isArabic) "اليابانية (JA)" else "Japanese (JA)"),
        "ko" to (if (isArabic) "الكورية (KO)" else "Korean (KO)"),
        "zh" to (if (isArabic) "الصينية (ZH)" else "Chinese (ZH)")
    )

    val modelOptions = ModelCatalog.ALL.map { m ->
        val ready = records[m.modelId]?.status == ModelStatus.READY
        val name = if (isArabic) m.displayNameAr else m.displayNameEn
        val suffix = if (ready) " ✓" else if (isArabic) " (غير مثبت)" else " (Missing)"
        m.modelId to "$name$suffix"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "إعداد التفريغ والترجمة" else "Processing Setup",
                subtitle = if (isArabic) "حدد اللغات ونموذج الذكاء الاصطناعي قبل البدء" else "Configure languages and AI model before starting",
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

        // Step 1: Source & Target Languages
        item {
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SetupWorkflowStepHeader(
                        step = "1",
                        icon = Icons.Rounded.Translate,
                        title = if (isArabic) "لغة الفيديو  •  لغة الترجمة" else "Source  •  Target Language"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "لغة الكلام في الفيديو" else "Video Speech Language",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        DropdownChoice(
                            options = sourceOptions,
                            selected = sourceLang,
                            onSelected = { sourceLang = it }
                        )
                        if (sourceLang == "auto") {
                            Text(
                                text = if (isArabic) "تحديد اللغة يدوياً يسرّع المعالجة ويحسّن الدقة." else "Selecting the spoken language manually can speed up processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.38f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "لغة الترجمة المستهدفة" else "Target Subtitle Language",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        SegmentedPillSelector(
                            options = listOf(
                                "ar" to (if (isArabic) "العربية" else "Arabic"),
                                "en" to (if (isArabic) "الإنجليزية" else "English")
                            ),
                            selected = targetLang,
                            onSelected = { targetLang = it }
                        )
                    }
                }
            }
        }

        // Step 2: AI Model Selection
        item {
            GlassCard(highlighted = !selectedReady) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SetupWorkflowStepHeader(
                        step = "2",
                        icon = Icons.Rounded.AutoAwesome,
                        title = if (isArabic) "اختيار نموذج الذكاء الاصطناعي" else "AI Recognition Model"
                    )

                    DropdownChoice(
                        options = modelOptions,
                        selected = selectedModel.modelId,
                        onSelected = { id ->
                            ModelCatalog.findById(id)?.let { viewModel.selectModel(it) }
                        }
                    )

                    if (selectedModel.highMemoryPolicy) {
                        Text(
                            text = if (isArabic)
                                "ملاحظة: هذا النموذج يتطلب ذاكرة RAM أعلى (${selectedModel.minRamMb} MB) وقد يستغرق وقتاً أطول."
                            else
                                "Note: This model requires higher RAM (${selectedModel.minRamMb} MB) and may take longer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalStatusColors.current.warning,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!selectedReady) {
                        FilledTonalButton(
                            onClick = onOpenModels,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (isArabic) "تثبيت النموذج من مكتبة النماذج" else "Install Model from Library",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            SecondaryActionButton(
                text = if (isArabic) "إدارة حزم الترجمة المحلية (ML Kit)" else "Manage Translation Packs (ML Kit)",
                onClick = onOpenTranslationPacks,
                icon = Icons.Rounded.Translate,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            PrimaryActionButton(
                text = if (isArabic) "بدء المعالجة" else "Start Processing",
                onClick = {
                    if (viewModel.startProcessing(app, sourceLang, targetLang)) {
                        onStartProcessing()
                    }
                },
                icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SetupWorkflowStepHeader(
    step: String,
    icon: ImageVector,
    title: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(scheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onPrimary,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(scheme.primaryContainer.copy(alpha = 0.65f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
