package com.transvibe.app.presentation.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.ScrollablePillTabs
import com.transvibe.app.presentation.components.SegmentedPillSelector
import com.transvibe.app.subtitles.SrtVttExporter
import kotlinx.coroutines.delay

@Composable
fun PlayerPreviewScreen(
    jobId: String,
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val jobMeta by viewModel.activeJobEntity.collectAsState()
    val cues by viewModel.activeCues.collectAsState()
    val mediaUriString = previewMediaUriForJob(jobId, jobMeta?.jobId, jobMeta?.mediaUriString)
    val currentCues = if (mediaUriString != null) cues else emptyList()
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    var editingCue by remember { mutableStateOf<SubtitleCue?>(null) }

    // Studio Subtitle Display Controls
    var displayMode by rememberSaveable { mutableStateOf("translated") }
    var fontSizeSp by rememberSaveable { mutableFloatStateOf(17f) }
    var backgroundStyle by rememberSaveable { mutableStateOf("compact") }
    var backgroundOpacity by rememberSaveable { mutableFloatStateOf(0.75f) }
    var textColorKey by rememberSaveable { mutableStateOf("white") }
    var verticalPosition by rememberSaveable { mutableFloatStateOf(0.86f) }
    var alignmentKey by rememberSaveable { mutableStateOf("center") }

    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(jobId) { viewModel.loadJobCues(jobId) }

    val player = remember(context) { ExoPlayer.Builder(context).build() }
    LaunchedEffect(player, mediaUriString) {
        player.stop()
        player.clearMediaItems()
        currentPosMs = 0L
        if (mediaUriString != null) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUriString)))
            player.prepare()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosMs = player.currentPosition
            delay(100L)
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            player.stop()
            player.release()
        }
    }

    val activeCue = currentCues.firstOrNull {
        val posUs = currentPosMs * 1000L
        posUs in it.startUs..it.endUs
    }
    val activeText = activeCue?.let { cueDisplayText(it, displayMode) }.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenHeroHeader(
            title = jobMeta?.mediaTitle ?: (if (isArabic) "معاينة الفيديو" else "Video Preview"),
            subtitle = jobMeta?.let { "${it.actualModelId} • ${it.targetLanguageSetting.uppercase()}" }
                ?: (if (isArabic) "عرض الفيديو مع الترجمة المتزامنة" else "Video with timed subtitles"),
            icon = Icons.Rounded.PlayCircle,
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

        // Pinned Studio Video Player Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            keepScreenOn = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AnimatedContent(
                    targetState = activeText,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "subtitle-fade"
                ) { subtitleText ->
                    if (subtitleText.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(verticalPosition.coerceIn(0.10f, 0.95f)))
                            StudioSubtitleOverlay(
                                text = subtitleText,
                                fontSizeSp = fontSizeSp,
                                alignment = studioSubtitleAlignment(alignmentKey),
                                backgroundStyle = backgroundStyle,
                                backgroundOpacity = backgroundOpacity,
                                textColor = studioSubtitleColor(textColorKey)
                            )
                            Spacer(modifier = Modifier.weight((1f - verticalPosition).coerceAtLeast(0.05f)))
                        }
                    }
                }
            }
        }

        // Studio Workspace Tabs
        ScrollablePillTabs(
            tabs = listOf(
                "0" to (if (isArabic) "جميع الترجمات" else "All Subtitles"),
                "1" to (if (isArabic) "مظهر الترجمة" else "Subtitle Style")
            ),
            selected = activeTab.toString(),
            onSelected = { activeTab = it.toIntOrNull() ?: 0 }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (activeTab == 0) {
                StudioSubtitleTimeline(
                    cues = currentCues,
                    active = activeCue,
                    displayMode = displayMode,
                    isArabic = isArabic,
                    onSeek = { player.seekTo(it.startUs / 1000L) },
                    onEdit = { editingCue = it }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "إعدادات عرض الترجمة" else "Subtitle Display Settings",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scheme.onSurface
                                )

                                Text(
                                    text = if (isArabic) "وضع العرض" else "Display Mode",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                SegmentedPillSelector(
                                    options = listOf(
                                        "original" to (if (isArabic) "الأصلي" else "Original"),
                                        "translated" to (if (isArabic) "المترجم" else "Translated"),
                                        "both" to (if (isArabic) "كلاهما" else "Both")
                                    ),
                                    selected = displayMode,
                                    onSelected = { displayMode = it }
                                )

                                StudioLabeledSlider(
                                    label = if (isArabic) "حجم الخط" else "Font Size",
                                    valueText = "${fontSizeSp.toInt()}sp",
                                    value = fontSizeSp,
                                    range = 12f..28f,
                                    onValue = { fontSizeSp = it }
                                )

                                Text(
                                    text = if (isArabic) "نمط الخلفية" else "Background Style",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ScrollablePillTabs(
                                    tabs = listOf(
                                        "compact" to (if (isArabic) "مدمج" else "Compact"),
                                        "band" to (if (isArabic) "شريط" else "Band"),
                                        "shadow" to (if (isArabic) "ظل فقط" else "Shadow"),
                                        "none" to (if (isArabic) "بدون" else "None")
                                    ),
                                    selected = backgroundStyle,
                                    onSelected = { backgroundStyle = it }
                                )

                                if (backgroundStyle == "compact" || backgroundStyle == "band") {
                                    StudioLabeledSlider(
                                        label = if (isArabic) "شفافية الخلفية" else "Background Opacity",
                                        valueText = "${(backgroundOpacity * 100).toInt()}%",
                                        value = backgroundOpacity,
                                        range = 0f..1f,
                                        onValue = { backgroundOpacity = it }
                                    )
                                }

                                Text(
                                    text = if (isArabic) "لون النص" else "Text Color",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                SegmentedPillSelector(
                                    options = listOf(
                                        "white" to (if (isArabic) "أبيض" else "White"),
                                        "yellow" to (if (isArabic) "أصفر" else "Yellow"),
                                        "cyan" to (if (isArabic) "سماوي" else "Cyan"),
                                        "green" to (if (isArabic) "أخضر" else "Green")
                                    ),
                                    selected = textColorKey,
                                    onSelected = { textColorKey = it }
                                )

                                StudioLabeledSlider(
                                    label = if (isArabic) "الموضع الرأسي" else "Vertical Position",
                                    valueText = "${(verticalPosition * 100).toInt()}%",
                                    value = verticalPosition,
                                    range = 0.10f..0.95f,
                                    onValue = { verticalPosition = it }
                                )

                                Text(
                                    text = if (isArabic) "المحاذاة" else "Alignment",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                SegmentedPillSelector(
                                    options = listOf(
                                        "start" to (if (isArabic) "بداية" else "Start"),
                                        "center" to (if (isArabic) "وسط" else "Center"),
                                        "end" to (if (isArabic) "نهاية" else "End")
                                    ),
                                    selected = alignmentKey,
                                    onSelected = { alignmentKey = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingCue?.let { cue ->
        StudioEditCueDialog(
            cue = cue,
            isArabic = isArabic,
            onDismiss = { editingCue = null },
            onSave = { orig, trans ->
                viewModel.editCue(cue.cueId, orig, trans)
                editingCue = null
            }
        )
    }
}

@Composable
private fun StudioSubtitleTimeline(
    cues: List<SubtitleCue>,
    active: SubtitleCue?,
    displayMode: String,
    isArabic: Boolean,
    onSeek: (SubtitleCue) -> Unit,
    onEdit: (SubtitleCue) -> Unit
) {
    val listState = rememberLazyListState()
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(active?.cueId, cues) {
        val index = cues.indexOfFirst { it.cueId == active?.cueId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (active != null) {
            GlassCard(highlighted = true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "${if (isArabic) "الترجمة الحالية" else "Current"} • ${SrtVttExporter.formatTimestamp(active.startUs, ',')}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary
                        )
                        Text(
                            text = cueDisplayText(active, displayMode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = { onEdit(active) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (isArabic) "تعديل" else "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cues, key = { it.cueId }) { cue ->
                    val isActive = cue.cueId == active?.cueId
                    Surface(
                        onClick = { onSeek(cue) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isActive) scheme.primaryContainer else scheme.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            if (isActive) scheme.primary else scheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${SrtVttExporter.formatTimestamp(cue.startUs, ',')} → ${SrtVttExporter.formatTimestamp(cue.endUs, ',')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) scheme.primary else scheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { onEdit(cue) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = if (isArabic) "تعديل" else "Edit",
                                        tint = if (isActive) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = cueDisplayText(cue, displayMode),
                                color = if (isActive) scheme.onPrimaryContainer else scheme.onSurface,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioLabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 44.dp)
        )
    }
}

@Composable
private fun StudioEditCueDialog(
    cue: SubtitleCue,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var original by remember(cue.cueId) { mutableStateOf(cue.effectiveOriginal) }
    var translated by remember(cue.cueId) { mutableStateOf(cue.effectiveTranslation.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "تعديل الترجمة" else "Edit Subtitle",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = original,
                    onValueChange = { original = it },
                    label = { Text(if (isArabic) "النص الأصلي" else "Original Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    value = translated,
                    onValueChange = { translated = it },
                    label = { Text(if (isArabic) "النص المترجم" else "Translated Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(original, translated.ifBlank { null }) },
                enabled = original.isNotBlank()
            ) {
                Text(if (isArabic) "حفظ" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun cueDisplayText(cue: SubtitleCue, mode: String): String {
    val original = cue.effectiveOriginal.trim()
    val translated = (cue.effectiveTranslation ?: cue.effectiveOriginal).trim()
    return when (mode) {
        "original" -> original
        "both" -> if (translated.isBlank() || translated == original) original else "$original\n$translated"
        else -> translated
    }
}

@Composable
private fun StudioSubtitleOverlay(
    text: String,
    fontSizeSp: Float,
    alignment: TextAlign,
    backgroundStyle: String,
    backgroundOpacity: Float,
    textColor: Color
) {
    val shadowOnly = backgroundStyle == "shadow"
    val lines = text.split('\n', limit = 2)
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (lines.size > 1) {
                StudioDirectedText(
                    text = lines.first(),
                    modifier = Modifier.fillMaxWidth(),
                    fontSizeSp = fontSizeSp * 0.78f,
                    alignment = alignment,
                    color = Color.White.copy(alpha = 0.74f),
                    shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1f, 1f), 2f)
                )
            }
            StudioDirectedText(
                text = lines.last(),
                modifier = Modifier.fillMaxWidth(),
                fontSizeSp = fontSizeSp,
                alignment = alignment,
                color = textColor,
                shadow = if (shadowOnly) Shadow(Color.Black, Offset(2f, 2f), 5f)
                else Shadow(Color.Black.copy(alpha = 0.65f), Offset(1f, 1f), 2f)
            )
        }
    }
    when (backgroundStyle) {
        "band" -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A).copy(alpha = (backgroundOpacity.coerceIn(0f, 1f) * 0.88f).coerceAtLeast(0.30f))
        ) { content() }
        "compact" -> Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A).copy(alpha = (backgroundOpacity.coerceIn(0f, 1f) * 0.88f).coerceAtLeast(0.30f))
        ) { content() }
        else -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun StudioDirectedText(
    text: String,
    modifier: Modifier,
    fontSizeSp: Float,
    alignment: TextAlign,
    color: Color,
    shadow: Shadow
) {
    CompositionLocalProvider(LocalLayoutDirection provides studioContentDirection(text)) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            textAlign = alignment,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.25f).sp,
            style = TextStyle(shadow = shadow)
        )
    }
}

private fun studioSubtitleColor(value: String): Color = when (value) {
    "yellow" -> Color(0xFFFFEB3B)
    "cyan" -> Color(0xFF80DEEA)
    "green" -> Color(0xFFA5D6A7)
    else -> Color.White
}

private fun studioSubtitleAlignment(value: String): TextAlign = when (value) {
    "start" -> TextAlign.Start
    "end" -> TextAlign.End
    else -> TextAlign.Center
}

private fun studioContentDirection(text: String): LayoutDirection {
    text.forEach { ch ->
        when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return LayoutDirection.Ltr
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return LayoutDirection.Rtl
        }
    }
    return LayoutDirection.Ltr
}
