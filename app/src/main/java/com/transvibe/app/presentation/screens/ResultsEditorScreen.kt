package com.transvibe.app.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.EmptyStateIllustration
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.PrimaryActionButton
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.theme.LocalStatusColors
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
    val scheme = MaterialTheme.colorScheme

    val srtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        if (uri != null) viewModel.exportSubtitles(uri, asVtt = false, bilingual = bilingualExport)
    }
    val vttLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vtt")) { uri ->
        if (uri != null) viewModel.exportSubtitles(uri, asVtt = true, bilingual = bilingualExport)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "النتائج ومحرر الترجمة" else "Results & Subtitle Editor",
                subtitle = jobMeta?.mediaTitle ?: (if (isArabic) "مراجعة المقاطع وتعديلها وتصديرها" else "Review, edit and export subtitles"),
                icon = Icons.Rounded.CheckCircle,
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

        // 2x2 Bento Summary Grid
        jobMeta?.let { meta ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultsBentoMetricCard(
                            icon = Icons.Rounded.Subtitles,
                            label = if (isArabic) "عدد المقاطع" else "Cues",
                            value = cues.size.toString(),
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                        ResultsBentoMetricCard(
                            icon = Icons.Rounded.Memory,
                            label = if (isArabic) "المحرك" else "Engine",
                            value = meta.actualEngineId,
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultsBentoMetricCard(
                            icon = Icons.Rounded.Psychology,
                            label = if (isArabic) "النموذج" else "Model",
                            value = meta.actualModelId,
                            modifier = Modifier.weight(1f)
                        )
                        ResultsBentoMetricCard(
                            icon = Icons.Rounded.VerifiedUser,
                            label = "SHA-256",
                            value = meta.modelSha256?.take(12) ?: "local",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Actions & Export Card
        item {
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isArabic) "المعاينة والتصدير" else "Preview & Export",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                    }

                    PrimaryActionButton(
                        text = if (isArabic) "معاينة الفيديو والترجمة" else "Open Video Player Preview",
                        onClick = onPreviewPlayer,
                        icon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { bilingualExport = !bilingualExport },
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.surfaceContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = bilingualExport,
                                onCheckedChange = { bilingualExport = it },
                                colors = CheckboxDefaults.colors(checkedColor = scheme.primary)
                            )
                            Text(
                                text = if (isArabic) "تصدير ثنائي اللغة (عربي + أصلي)" else "Bilingual export (Original + Translated)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryActionButton(
                            text = "SRT",
                            onClick = { srtLauncher.launch("transvibe.srt") },
                            icon = Icons.Rounded.SaveAlt,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryActionButton(
                            text = "VTT",
                            onClick = { vttLauncher.launch("transvibe.vtt") },
                            icon = Icons.Rounded.SaveAlt,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryActionButton(
                            text = "MP4",
                            onClick = { viewModel.requestVideoBurnInCheck() },
                            icon = Icons.Rounded.VideoLibrary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryActionButton(
                            text = if (isArabic) "إعادة الترجمة" else "Re-translate",
                            onClick = { viewModel.retranslateActiveJob("ar") },
                            icon = Icons.Rounded.Translate,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryActionButton(
                            text = if (isArabic) "تراجع" else "Undo",
                            onClick = { viewModel.undoCueEdit() },
                            enabled = viewModel.canUndoCueEdit(),
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (cues.isEmpty()) {
            item {
                EmptyStateIllustration(
                    icon = Icons.Rounded.Subtitles,
                    title = if (isArabic) "لا توجد مقاطع ترجمة" else "No Subtitle Cues",
                    subtitle = if (isArabic) "لم يتم العثور على مقاطع محفوظة لهذه الجلسة." else "No saved subtitle cues were found for this job."
                )
            }
        } else {
            items(cues, key = { it.cueId }) { cue ->
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = scheme.primaryContainer
                            ) {
                                Text(
                                    text = "${SrtVttExporter.formatTimestamp(cue.startUs, ',')} → ${SrtVttExporter.formatTimestamp(cue.endUs, ',')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (cue.warnings.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LocalStatusColors.current.warningContainer
                                ) {
                                    Text(
                                        text = cue.warnings.joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalStatusColors.current.warning,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = cue.effectiveOriginal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = cue.effectiveTranslation ?: "",
                            onValueChange = { viewModel.editCue(cue.cueId, null, it) },
                            label = { Text(if (isArabic) "الترجمة العربية" else "Arabic Subtitle") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsBentoMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier = modifier, highlighted = highlighted) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
