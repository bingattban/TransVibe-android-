package com.transvibe.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transvibe.app.presentation.theme.GlassCard
import com.transvibe.app.presentation.theme.TransVibeColors

@Composable
fun WelcomeScreen(
    isArabic: Boolean,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TransVibe",
            color = TransVibeColors.ElectricCyan,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isArabic)
                    "تفريغ وترجمة الفيديو إلى العربية محلياً بالكامل على جهازك دون رفع أي ملفات."
                else
                    "Offline-first video transcription and Arabic subtitling directly on your device.",
                color = TransVibeColors.TextPrimary
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onSelectLanguage("ar") }, modifier = Modifier.weight(1f)) {
                    Text("العربية (RTL)")
                }
                OutlinedButton(onClick = { onSelectLanguage("en") }, modifier = Modifier.weight(1f)) {
                    Text("English")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = TransVibeColors.ElectricCyan)
        ) {
            Text(if (isArabic) "بدء الاستخدام" else "Get Started", color = TransVibeColors.MidnightNavy)
        }
    }
}
