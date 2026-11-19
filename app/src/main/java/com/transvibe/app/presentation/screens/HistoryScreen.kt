package com.transvibe.app.presentation.screens

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.transvibe.app.domain.JobState
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun HistoryScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isArabic) "المشاريع والسجل" else "Projects & History", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs, key = { it.jobId }) { job ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenJob(job.jobId) }
                ) {
                    Text(job.mediaTitle)
                    Text("${job.actualModelId} • ${job.state}", color = TransVibeColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (job.state == JobState.INTERRUPTED_RECOVERABLE.name || job.state == JobState.FAILED.name) {
                            OutlinedButton(onClick = {
                                viewModel.startProcessing(app, job.sourceLanguageSetting, job.targetLanguageSetting, resumeJobId = job.jobId)
                            }) {
                                Text(if (isArabic) "استئناف" else "Resume", color = TransVibeColors.ElectricCyan)
                            }
                        }
                        OutlinedButton(onClick = { viewModel.deleteJob(job.jobId) }) {
                            Text(if (isArabic) "حذف المشروع" else "Delete", color = TransVibeColors.CoralError)
                        }
                    }
                }
            }
        }
    }
}
