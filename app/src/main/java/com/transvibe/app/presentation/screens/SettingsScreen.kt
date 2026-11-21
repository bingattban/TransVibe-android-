package com.transvibe.app.presentation.screens

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transvibe.app.persistence.UserPreferences
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.components.SegmentedPillSelector

@Composable
fun SettingsScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenTranslationPacks: () -> Unit,
    onAbout: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val cacheMb by viewModel.cacheSizeMb.collectAsState()
    val prefs by viewModel.preferences.collectAsState(initial = UserPreferences())
    var diagPreview by remember { mutableStateOf<String?>(null) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize(app)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeroHeader(
                title = if (isArabic) "الإعدادات والخصوصية" else "Settings & Privacy",
                subtitle = if (isArabic) "اللغة، المظهر، الموارد، والتخزين المحلي" else "Language, appearance, models & local storage",
                icon = Icons.Rounded.Settings
            )
        }

        // Language & Appearance
        item {
            SettingsSectionLabel(if (isArabic) "اللغة والمظهر" else "Language & Appearance")
        }

        item {
            SettingsCard(
                title = if (isArabic) "لغة التطبيق" else "Application Language",
                icon = Icons.Rounded.Language
            ) {
                SegmentedPillSelector(
                    options = listOf("ar" to "العربية", "en" to "English"),
                    selected = prefs.uiLanguage,
                    onSelected = { viewModel.setUiLanguage(it) }
                )
            }
        }

        item {
            SettingsCard(
                title = if (isArabic) "المظهر والألوان" else "Appearance",
                icon = Icons.Rounded.Palette
            ) {
                SegmentedPillSelector(
                    options = listOf(
                        "system" to (if (isArabic) "النظام" else "System"),
                        "light" to (if (isArabic) "نهاري" else "Light"),
                        "dark" to (if (isArabic) "ليلي" else "Dark")
                    ),
                    selected = prefs.themeMode,
                    onSelected = { viewModel.setThemeMode(it) }
                )
                Text(
                    text = if (isArabic) "يُحفظ اختيار المظهر تلقائياً ويعمل فوراً." else "Your theme selection is saved automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        // Models & Translation
        item {
            SettingsSectionLabel(if (isArabic) "النماذج والترجمة" else "Models & Translation")
        }

        item {
            SettingsCard(
                title = if (isArabic) "الموارد المحلية للذكاء الاصطناعي" else "Offline AI Resources",
                icon = Icons.Rounded.Psychology
            ) {
                Text(
                    text = if (isArabic) "إدارة النماذج السبعة المعتمدة وحزم الترجمة المحلية دون رفع أي بيانات." else "Manage the 7 supported ASR models and on-device translation packs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = onOpenModels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (isArabic) "إدارة النماذج السبعة" else "Manage 7 Models",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                SecondaryActionButton(
                    text = if (isArabic) "حزم الترجمة المحلية (ML Kit)" else "Offline Translation Packs",
                    onClick = onOpenTranslationPacks,
                    icon = Icons.Rounded.Translate,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Storage & Diagnostics
        item {
            SettingsSectionLabel(if (isArabic) "التخزين والتشخيص" else "Storage & Diagnostics")
        }

        item {
            SettingsCard(
                title = if (isArabic) "الملفات المؤقتة والتخزين" else "Temporary Cache & Storage",
                icon = Icons.Rounded.Storage
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.surfaceContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "حجم الذاكرة المؤقتة والتجهيز" else "Temp & Staging Cache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                        Text(
                            text = "$cacheMb MB",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.primary
                        )
                    }
                }
                SecondaryActionButton(
                    text = if (isArabic) "مسح الملفات المؤقتة" else "Clear Temp Cache",
                    onClick = { viewModel.clearAppCache(app) },
                    icon = Icons.Rounded.DeleteSweep,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            SettingsCard(
                title = if (isArabic) "التقرير التشخيصي المنقّح" else "Redacted Diagnostics",
                icon = Icons.Rounded.BugReport
            ) {
                SecondaryActionButton(
                    text = if (isArabic) "إنشاء تقرير تشخيصي منقّح" else "Generate Redacted Report",
                    onClick = { diagPreview = viewModel.buildRedactedDiagnosticText() },
                    icon = Icons.Rounded.BugReport,
                    modifier = Modifier.fillMaxWidth()
                )
                diagPreview?.let { report ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surfaceContainer
                    ) {
                        Text(
                            text = report,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // About
        item {
            SettingsSectionLabel(if (isArabic) "حول التطبيق" else "About")
        }

        item {
            SettingsCard(
                title = if (isArabic) "حول TransVibe" else "About TransVibe",
                icon = Icons.Rounded.Info,
                onClick = onAbout
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = scheme.primary
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            content()
        }
    }
}
