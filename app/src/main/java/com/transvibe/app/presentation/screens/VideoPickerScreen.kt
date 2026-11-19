package com.transvibe.app.presentation.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun VideoPickerScreen(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onProceedToSetup: () -> Unit
) {
    val context = LocalContext.current.applicationContext as Application
    val inspected by viewModel.inspectedMedia.collectAsState()
    val selectedTrack by viewModel.selectedTrackIndex.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.inspectSelectedVideo(context, uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isArabic) "اختيار وفحص الفيديو" else "Video Picker & Inspector", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        }

        Button(
            onClick = { pickerLauncher.launch(arrayOf("video/*", "audio/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isArabic) "فتح مستعرض الملفات (SAF)" else "Choose Local Video (SAF)")
        }

        inspected?.let { media ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(media.displayName, color = TransVibeColors.ElectricCyan)
                Text("Duration: ${media.durationUs / 1_000_000L} s")
                Text(if (isArabic) "المسارات الصوتية:" else "Audio Tracks:")
                media.audioTracks.forEach { track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedTrack == track.trackIndex,
                            onClick = { viewModel.selectAudioTrack(track.trackIndex) }
                        )
                        Text("#${track.trackIndex} ${track.mimeType} (${track.sampleRateHz} Hz, ${track.channelCount}ch)")
                    }
                }
            }
            Button(onClick = onProceedToSetup, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "التالي: إعداد المعالجة" else "Next: Processing Setup")
            }
        }
    }
}
