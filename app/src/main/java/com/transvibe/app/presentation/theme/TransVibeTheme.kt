package com.transvibe.app.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
    val LightCyan = Color(0xFF0284C7)
    val LightSurface = Color(0xFFF8FAFC)
    val LightOnSurface = Color(0xFF0F172A)
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

private val LightScheme = lightColorScheme(
    primary = TransVibeColors.LightCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF082F49),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = TransVibeColors.LightSurface,
    onBackground = TransVibeColors.LightOnSurface,
    surface = Color.White,
    onSurface = TransVibeColors.LightOnSurface,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun TransVibeTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (isDark) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, borderColor)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun TransVibeBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(
                colors.background,
                colors.primaryContainer.copy(alpha = 0.14f),
                colors.background
            ))
        )
    ) { content() }
}
