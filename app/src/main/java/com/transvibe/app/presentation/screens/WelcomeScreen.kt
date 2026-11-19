package com.transvibe.app.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.size
import com.transvibe.app.R
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(180.dp).background(
                Brush.radialGradient(listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                    MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )), CircleShape
            ).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.transvibe_welcome_logo),
                contentDescription = if (isArabic) "شعار TransVibe" else "TransVibe logo",
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "TransVibe",
            color = MaterialTheme.colorScheme.primary,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isArabic)
                    "تفريغ وترجمة الفيديو إلى العربية محلياً بالكامل على جهازك دون رفع أي ملفات."
                else
                    "Offline-first video transcription and Arabic subtitling directly on your device.",
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(if (isArabic) "🔒 معالجة محلية تحفظ خصوصيتك" else "🔒 Private on-device processing")
            Text(if (isArabic) "🎞 تفريغ الصوت وتحرير الترجمة" else "🎞 Transcribe and edit timed subtitles")
            Text(if (isArabic) "💾 تصدير SRT وVTT" else "💾 Export SRT and VTT")
            Spacer(Modifier.height(14.dp))
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
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isArabic) "بدء الاستخدام" else "Get Started", color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold)
        }
    }
}
