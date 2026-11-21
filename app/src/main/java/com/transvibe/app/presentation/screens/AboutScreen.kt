package com.transvibe.app.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.transvibe.app.BuildConfig
import com.transvibe.app.R
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.ScreenHeroHeader

private data class AboutTableRow(
    val name: String,
    val version: String,
    val purpose: String,
    val source: String,
    val license: String
)

private val aboutColumnWidths = listOf(210.dp, 90.dp, 260.dp, 140.dp, 180.dp)

@Composable
fun AboutScreen(
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val modelRows = listOf(
        AboutTableRow(
            if (isArabic) "نماذج Whisper (5 أحجام)" else "Whisper Models (5 sizes)",
            "GGML",
            if (isArabic) "التعرف الصوتي متعدد اللغات محلياً" else "Offline multilingual speech recognition",
            "OpenAI",
            "MIT"
        ),
        AboutTableRow(
            "Moonshine Base Arabic",
            "ONNX",
            if (isArabic) "تفريغ صوتي عربي سريع عبر Sherpa-ONNX" else "Fast Arabic ASR via Sherpa-ONNX",
            "Useful Sensors / k2-fsa",
            "Apache-2.0 / MIT"
        ),
        AboutTableRow(
            "SenseVoice Small INT8",
            "INT8",
            if (isArabic) "تعرف صوتي سريع متعدد اللغات" else "Fast multilingual ASR",
            "FunAudioLLM / k2-fsa",
            "Apache-2.0"
        ),
        AboutTableRow(
            "Silero VAD",
            "ONNX",
            if (isArabic) "كشف مقاطع الكلام وتقسيم الصوت" else "Voice activity detection & segmentation",
            "Silero",
            "MIT"
        ),
        AboutTableRow(
            "Google ML Kit Translation",
            "17.0.3",
            if (isArabic) "الترجمة النصية المحلية بدون إنترنت" else "On-device offline subtitle translation",
            "Google",
            if (isArabic) "ترخيص Google" else "Google Terms"
        )
    )
    val toolRows = listOf(
        AboutTableRow("whisper.cpp", "JNI", if (isArabic) "محرك تشغيل نماذج Whisper" else "Native Whisper inference engine", "ggerganov", "MIT"),
        AboutTableRow("Sherpa-ONNX", "AAR", if (isArabic) "تشغيل Moonshine و SenseVoice" else "Moonshine & SenseVoice runtime", "k2-fsa", "Apache-2.0"),
        AboutTableRow("Android Media3 / ExoPlayer", "1.5.1", if (isArabic) "تشغيل الفيديو ومعاينة الترجمة" else "Video playback & subtitle preview", "Google / Android", "Apache-2.0"),
        AboutTableRow("Jetpack Compose & Material 3", "BOM", if (isArabic) "بناء واجهة المستخدم الحديثة" else "Modern declarative UI toolkit", "Google / Android", "Apache-2.0"),
        AboutTableRow("Room & DataStore", "2.8.5", if (isArabic) "حفظ السجل والمقاطع والتفضيلات محلياً" else "Local jobs, cues & preferences storage", "Google / Android", "Apache-2.0")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "حول التطبيق" else "About",
                subtitle = "TransVibe v${BuildConfig.VERSION_NAME}",
                icon = Icons.Rounded.Info,
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
            GlassCard(highlighted = true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.transvibe_welcome_logo),
                            contentDescription = "TransVibe",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, scheme.primary.copy(alpha = 0.5f), CircleShape)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TransVibe",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = scheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = scheme.primaryContainer,
                                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.45f))

                    Text(
                        text = if (isArabic) "فكرة التطبيق والخصوصية" else "Privacy & Local Processing",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )
                    Text(
                        text = if (isArabic)
                            "معالجة محلية 100% على جهازك لتفريغ الفيديوهات وترجمتها وتصدير ملفات SRT و WebVTT دون رفع أي ملفات إلى الإنترنت."
                        else
                            "100% on-device offline video transcription, Arabic subtitle translation, and SRT/WebVTT export with zero cloud uploads.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "Package: ${BuildConfig.APPLICATION_ID}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    if (BuildConfig.DIAGNOSTIC_NATIVE_ALLOWED) {
                        Text(
                            text = if (isArabic)
                                "DEBUG / PREVIEW: وضع تشخيصي؛ بناء التطبيق لا يعني أن جميع محركات التعرف الصوتي مفعّلة."
                            else
                                "DEBUG / PREVIEW: Diagnostic mode active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isArabic) "نماذج الذكاء الاصطناعي" else "AI Models",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            AboutSpecTable(
                rows = modelRows,
                nameHeader = if (isArabic) "النموذج" else "Model",
                isArabic = isArabic
            )
        }

        item {
            Text(
                text = if (isArabic) "الأدوات والمكتبات" else "Libraries & Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            AboutSpecTable(
                rows = toolRows,
                nameHeader = if (isArabic) "الأداة" else "Library",
                isArabic = isArabic
            )
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }
}

@Composable
private fun AboutSpecTable(rows: List<AboutTableRow>, nameHeader: String, isArabic: Boolean) {
    GlassCard {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = if (isArabic) "اسحب أفقياً لعرض كامل الجدول" else "Swipe horizontally to view full table",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val headers = listOf(
                nameHeader,
                if (isArabic) "الإصدار" else "Version",
                if (isArabic) "الوظيفة" else "Purpose",
                if (isArabic) "المصدر" else "Source",
                if (isArabic) "الترخيص" else "License"
            )
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AboutTableLine(headers, header = true, alternate = false)
                rows.forEachIndexed { index, row ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    AboutTableLine(
                        listOf(row.name, row.version, row.purpose, row.source, row.license),
                        header = false,
                        alternate = index % 2 == 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutTableLine(cells: List<String>, header: Boolean, alternate: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        header -> scheme.primaryContainer.copy(alpha = 0.65f)
        alternate -> scheme.surfaceContainer.copy(alpha = 0.55f)
        else -> scheme.surfaceContainerLow
    }
    Row(modifier = Modifier.background(background), verticalAlignment = Alignment.CenterVertically) {
        cells.forEachIndexed { index, text ->
            AboutTableCell(
                value = text,
                width = aboutColumnWidths[index],
                isHeader = header
            )
        }
    }
}

@Composable
private fun AboutTableCell(value: String, width: Dp, isHeader: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = value,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        color = if (isHeader) scheme.onPrimaryContainer else scheme.onSurface
    )
}
