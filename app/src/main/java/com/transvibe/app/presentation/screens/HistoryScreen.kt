package com.transvibe.app.presentation.screens

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.JobState
import com.transvibe.app.persistence.JobEntity
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.components.EmptyStateIllustration
import com.transvibe.app.presentation.components.GlassCard
import com.transvibe.app.presentation.components.MetadataBadge
import com.transvibe.app.presentation.components.ScreenHeroHeader
import com.transvibe.app.presentation.components.ScrollablePillTabs
import com.transvibe.app.presentation.components.StatusPill
import com.transvibe.app.presentation.theme.LocalStatusColors

@Composable
fun HistoryScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDelete by remember { mutableStateOf<Set<String>?>(null) }
    var filter by remember { mutableIntStateOf(0) }

    val filtered = remember(jobs, filter) {
        when (filter) {
            1 -> jobs.filter { it.state == JobState.COMPLETED.name }
            2 -> jobs.filter { it.state in setOf(JobState.CANCELLED.name, JobState.INTERRUPTED_RECOVERABLE.name) }
            3 -> jobs.filter { it.state == JobState.FAILED.name }
            else -> jobs
        }
    }

    val completedCount = remember(jobs) { jobs.count { it.state == JobState.COMPLETED.name } }
    val interruptedCount = remember(jobs) {
        jobs.count { it.state in setOf(JobState.CANCELLED.name, JobState.INTERRUPTED_RECOVERABLE.name) }
    }
    val failedCount = remember(jobs) { jobs.count { it.state == JobState.FAILED.name } }

    val filterOptions = listOf(
        "0" to "${if (isArabic) "الكل" else "All"} (${jobs.size})",
        "1" to "${if (isArabic) "مكتمل" else "Completed"} ($completedCount)",
        "2" to "${if (isArabic) "متوقف" else "Interrupted"} ($interruptedCount)",
        "3" to "${if (isArabic) "فشل" else "Failed"} ($failedCount)"
    )

    if (pendingDelete != null) {
        val ids = pendingDelete.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    text = if (isArabic) "تأكيد الحذف" else "Confirm Deletion",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = if (ids.size == 1) {
                        if (isArabic) "هل تريد حذف هذا المشروع من السجل؟" else "Delete this project from history?"
                    } else {
                        if (isArabic) "هل تريد حذف ${ids.size} مشاريع محددة؟" else "Delete ${ids.size} selected projects?"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ids.forEach { viewModel.deleteJob(it) }
                        selected = selected - ids
                        pendingDelete = null
                    }
                ) {
                    Text(
                        text = if (isArabic) "حذف" else "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val isAllSelected = jobs.isNotEmpty() && selected.size == jobs.size
            ScreenHeroHeader(
                title = if (isArabic) "المشاريع والسجل" else "Projects & History",
                subtitle = if (isArabic) "الملفات المعالجة والنتائج المحفوظة" else "Saved processing jobs & results",
                icon = Icons.Rounded.History,
                trailing = if (jobs.isNotEmpty()) {
                    {
                        OutlinedButton(
                            onClick = {
                                selected = if (isAllSelected) emptySet() else jobs.mapTo(linkedSetOf()) { it.jobId }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isAllSelected) {
                                    if (isArabic) "إلغاء التحديد" else "Clear"
                                } else {
                                    if (isArabic) "تحديد الكل" else "Select All"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                } else null
            )
        }

        item {
            ScrollablePillTabs(
                tabs = filterOptions,
                selected = filter.toString(),
                onSelected = { filter = it.toIntOrNull() ?: 0 }
            )
        }

        item {
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selected.size} • ${if (isArabic) "حذف المحدد" else "Delete Selected"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { pendingDelete = selected },
                            modifier = Modifier.heightIn(min = 42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isArabic) "حذف" else "Delete",
                                modifier = Modifier.padding(start = 6.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.62f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateIllustration(
                        icon = Icons.Rounded.History,
                        title = if (jobs.isEmpty()) {
                            if (isArabic) "لا توجد مشاريع محفوظة بعد" else "No Saved Projects Yet"
                        } else {
                            if (isArabic) "لا توجد نتائج لهذا الفلتر" else "No Matching Projects"
                        },
                        subtitle = if (jobs.isEmpty()) {
                            if (isArabic) "ابدأ بمعالجة أول فيديو وستظهر نتائجه هنا تلقائياً." else "Process your first video and it will appear here."
                        } else {
                            if (isArabic) "جرّب اختيار تصنيف آخر لعرض المشاريع." else "Try another status filter to view projects."
                        }
                    )
                }
            }
        } else {
            items(filtered, key = { it.jobId }) { job ->
                HistoryJobCard(
                    job = job,
                    selected = job.jobId in selected,
                    isArabic = isArabic,
                    onToggle = {
                        selected = if (job.jobId in selected) selected - job.jobId else selected + job.jobId
                    },
                    onOpen = { onOpenJob(job.jobId) },
                    onResume = {
                        viewModel.startProcessing(
                            app,
                            job.sourceLanguageSetting,
                            job.targetLanguageSetting,
                            resumeJobId = job.jobId
                        )
                    },
                    onDelete = { pendingDelete = setOf(job.jobId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryJobCard(
    job: JobEntity,
    selected: Boolean,
    isArabic: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalStatusColors.current
    val pillColor = when (job.state) {
        JobState.COMPLETED.name -> colors.ready
        JobState.FAILED.name -> colors.error
        JobState.CANCELLED.name, JobState.INTERRUPTED_RECOVERABLE.name -> colors.warning
        else -> colors.processing
    }
    val canResume = job.state == JobState.INTERRUPTED_RECOVERABLE.name || job.state == JobState.FAILED.name

    GlassCard(highlighted = selected) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(scheme.primary.copy(alpha = 0.28f), scheme.primaryContainer)
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoFile,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = job.mediaTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetadataBadge(Icons.Rounded.Memory, job.actualModelId)
                        MetadataBadge(
                            Icons.Rounded.Translate,
                            "${job.sourceLanguageSetting.uppercase()} → ${job.targetLanguageSetting.uppercase()}"
                        )
                    }
                }

                StatusPill(label = localizedJobState(job.state, isArabic), color = pillColor)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        scheme.surfaceContainer.copy(alpha = 0.65f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = scheme.primary)
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            job.createdAtMs,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (canResume) {
                        IconButton(
                            onClick = onResume,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.warning)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(colors.warningContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = if (isArabic) "استئناف" else "Resume",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onOpen,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = scheme.primary)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(scheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = if (isArabic) "فتح" else "Open",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = scheme.error)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(scheme.errorContainer.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = if (isArabic) "حذف" else "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
