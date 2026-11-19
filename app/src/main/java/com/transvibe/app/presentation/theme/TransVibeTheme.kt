package com.transvibe.app.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TransVibeColors {
    val MidnightNavy = Color(0xFF090D16)
    val DeepSurface = Color(0xFF111827)
    val CardSurface = Color(0xCC162033)
    val CardBorder = Color(0x3338BDF8)
    val ElectricCyan = Color(0xFF38BDF8)
    val WarmGold = Color(0xFFFBBF24)
    val EmeraldSuccess = Color(0xFF34D399)
    val CoralError = Color(0xFFF87171)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
}

private val DarkScheme = darkColorScheme(
    primary = TransVibeColors.ElectricCyan,
    onPrimary = TransVibeColors.MidnightNavy,
    secondary = TransVibeColors.WarmGold,
    onSecondary = TransVibeColors.MidnightNavy,
    background = TransVibeColors.MidnightNavy,
    onBackground = TransVibeColors.TextPrimary,
    surface = TransVibeColors.DeepSurface,
    onSurface = TransVibeColors.TextPrimary,
    error = TransVibeColors.CoralError,
    onError = TransVibeColors.MidnightNavy
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TransVibeColors.TextPrimary),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TransVibeColors.TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, color = TransVibeColors.TextPrimary),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TransVibeColors.TextSecondary)
)

@Composable
fun TransVibeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = TransVibeColors.CardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransVibeColors.CardSurface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
