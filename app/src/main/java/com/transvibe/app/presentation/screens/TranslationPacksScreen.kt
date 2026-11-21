package com.transvibe.app.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.SecondaryActionButton
import com.transvibe.app.presentation.components.SectionHeader
import com.transvibe.app.presentation.components.StatusPill
import com.transvibe.app.presentation.theme.LocalStatusColors
import com.transvibe.app.translation.PackTransferState
import com.transvibe.app.translation.TranslationPackInfo

@Composable
fun TranslationPacksScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val packs by viewModel.translationPacks.collectAsState()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { viewModel.refreshTranslationPacks() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            ScreenHeroHeader(
                title = if (isArabic) "حزم الترجمة" else "Translation Packs",
                subtitle = if (isArabic) "إدارة حزم الترجمة المحلية (ML Kit)" else "Manage on-device translation packs (ML Kit)",
                icon = Icons.Rounded.Translate,
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

        TranslationPacksListContent(
            packs = packs,
            isArabic = isArabic,
            onRetryPending = { viewModel.retryPendingTranslationPacks() },
            onDownload = { viewModel.downloadTranslationPack(it) },
            onDelete = { viewModel.deleteTranslationPack(it) }
        )
    }
}

@Composable
internal fun TranslationPacksListContent(
    packs: List<TranslationPackInfo>,
    isArabic: Boolean,
    onRetryPending: () -> Unit,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val statusColors = LocalStatusColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (isArabic) "حزم اللغات المحلية" else "Offline Language Packs",
                subtitle = if (isArabic) "تُستخدم للترجمة التلقائية بدون إنترنت" else "Used for offline on-device subtitle translation"
            )
        }

        item {
            SecondaryActionButton(
                text = if (isArabic) "إعادة محاولة التنزيلات المعلقة" else "Retry Pending Downloads",
                onClick = onRetryPending,
                icon = Icons.Rounded.Refresh,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(packs, key = { "pack-${it.code}" }) { pack ->
            val stateColor = when (pack.state) {
                PackTransferState.INSTALLED -> statusColors.ready
                PackTransferState.FAILED -> statusColors.error
                PackTransferState.DOWNLOADING -> statusColors.processing
                else -> statusColors.warning
            }
            val stateLabel = when (pack.state) {
                PackTransferState.INSTALLED -> if (isArabic) "جاهز" else "Installed"
                PackTransferState.DOWNLOADING -> if (isArabic) "جاري التنزيل" else "Downloading"
                PackTransferState.PENDING_NETWORK -> if (isArabic) "بانتظار الشبكة" else "Pending"
                PackTransferState.FAILED -> if (isArabic) "فشل" else "Failed"
                PackTransferState.NOT_INSTALLED -> if (isArabic) "غير مثبت" else "Missing"
            }

            GlassCard(highlighted = pack.offlineReady) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(scheme.primary.copy(alpha = 0.25f), scheme.primaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${if (isArabic) pack.nameAr else pack.nameEn} (${pack.code.uppercase()})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface
                                )
                                if (pack.isCore) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = scheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (isArabic) "أساسية" else "CORE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = scheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = pack.sizeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }

                        StatusPill(label = stateLabel, color = stateColor)
                    }

                    if (pack.state == PackTransferState.DOWNLOADING) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(100.dp)),
                            trackColor = scheme.surfaceContainerHighest,
                            color = scheme.primary
                        )
                    }

                    pack.lastError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.error
                        )
                    }

                    if (!pack.offlineReady) {
                        Button(
                            onClick = { onDownload(pack.code) },
                            enabled = pack.state != PackTransferState.DOWNLOADING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (pack.state == PackTransferState.FAILED) {
                                    if (isArabic) "إعادة المحاولة" else "Retry"
                                } else {
                                    if (isArabic) "تنزيل الحزمة" else "Download Pack"
                                },
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onDownload(pack.code) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "فحص التحديث" else "Check Update",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            OutlinedButton(
                                onClick = { onDelete(pack.code) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
                                border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = if (isArabic) "حذف" else "Remove",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
