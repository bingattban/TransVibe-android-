package com.transvibe.app.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
fun BenchmarkScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val summaries by viewModel.benchmarkSummaries.collectAsState()
    val freezeRecords by viewModel.freezeRecords.collectAsState()
    val tinyFreeze = freezeRecords[ModelCatalog.WHISPER_TINY_ID]
    var corpusUri by remember { mutableStateOf<Uri?>(null) }
    val scheme = MaterialTheme.colorScheme
    val statusColors = LocalStatusColors.current

    val corpusPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) corpusUri = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "دراسة أداء Whisper Tiny" else "Tiny Benchmark Study",
                subtitle = if (isArabic) "مقارنة زمن الاستدلال واستهلاك الذاكرة (2/3/4 خيوط)" else "Measure real inference latency & memory (2/3/4 threads)",
                icon = Icons.Rounded.Speed,
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
            GlassCard(highlighted = tinyFreeze?.approvedByOwner == true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "حالة اعتماد Whisper Tiny" else "Whisper Tiny Freeze Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        StatusPill(
                            label = tinyFreeze?.status ?: "UNDER_DEVELOPMENT",
                            color = if (tinyFreeze?.approvedByOwner == true) statusColors.ready else statusColors.warning
                        )
                    }
                    Text(
                        text = (if (isArabic) "موافقة المالك: " else "Owner Approved: ") + (tinyFreeze?.approvedByOwner ?: false),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = (if (isArabic) "المقطع المرجعي: " else "Reference Clip: ") +
                            (corpusUri?.lastPathSegment ?: if (isArabic) "لم يُحدد بعد" else "None selected"),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryActionButton(
                    text = if (isArabic) "اختيار مقطع صوتي مرجعي" else "Select Reference Corpus Clip",
                    onClick = { corpusPicker.launch(arrayOf("audio/*", "video/*")) },
                    icon = Icons.Rounded.Audiotrack,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryActionButton(
                    text = if (isArabic) "تشغيل اختبار استدلال حقيقي (2/3/4 خيوط)" else "Run Real Inference Study (2/3/4 Threads)",
                    onClick = { viewModel.runTinyBenchmarkStudy(corpusUri) },
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(summaries, key = { it.threads }) { s ->
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isArabic) "${s.threads} خيوط (${s.runCount} تشغيلات)" else "Threads: ${s.threads} (${s.runCount} runs)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataBadge(Icons.Rounded.Timer, "Median: ${s.medianInferenceMs} ms", highlighted = true)
                        MetadataBadge(Icons.Rounded.Speed, "p90: ${s.p90InferenceMs} ms")
                        MetadataBadge(Icons.Rounded.Memory, "Peak PSS: ${s.maxPeakPssKb} KB")
                    }
                }
            }
        }

        if (summaries.isNotEmpty() && tinyFreeze?.approvedByOwner != true) {
            item {
                PrimaryActionButton(
                    text = if (isArabic) "اعتماد وتجميد Whisper Tiny (موافقة المالك)" else "Owner Approve & Freeze Tiny",
                    onClick = { viewModel.approveModelFreeze(ModelCatalog.WHISPER_TINY_ID) },
                    icon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
