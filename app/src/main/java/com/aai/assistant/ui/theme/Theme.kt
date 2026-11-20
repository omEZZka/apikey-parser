package com.aai.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgDark = Color(0xFF07090F)
val BgElevated = Color(0xFF0C101A)
val SurfaceDark = Color(0xFF111624)
val SurfaceVariantDark = Color(0xFF1A2133)
val SurfaceHigh = Color(0xFF222B42)
val AccentViolet = Color(0xFF8B7CFF)
val AccentCyan = Color(0xFF4FD8EB)
val AccentBlue = Color(0xFF5EA2FF)
val AccentGreen = Color(0xFF42D392)
val AccentAmber = Color(0xFFF5B84F)
val AccentRed = Color(0xFFFF6B6B)
val TextPrimary = Color(0xFFEDF1FA)
val TextSecondary = Color(0xFF97A0B5)
val TextMuted = Color(0xFF5C6579)
val OutlineDim = Color(0xFF252E45)

val AgentGradient = Brush.linearGradient(listOf(Color(0xFF7C6CFF), Color(0xFF4FD8EB)))
val SendGradient = Brush.linearGradient(listOf(Color(0xFF8B7CFF), Color(0xFF5EA2FF)))
val DangerGradient = Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF9E6B)))

private val AaiColors = darkColorScheme(
    primary = AccentViolet,
    onPrimary = Color(0xFF0B0716),
    primaryContainer = Color(0xFF2A2450),
    onPrimaryContainer = Color(0xFFDCD6FF),
    secondary = AccentCyan,
    onSecondary = Color(0xFF06222A),
    secondaryContainer = Color(0xFF123A44),
    onSecondaryContainer = Color(0xFFBFF3FB),
    tertiary = AccentGreen,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = BgElevated,
    surfaceContainerHigh = SurfaceHigh,
    error = AccentRed,
    onError = Color(0xFF2B0B0B),
    errorContainer = Color(0xFF47202A),
    onErrorContainer = Color(0xFFFFC9C9),
    outline = OutlineDim,
    outlineVariant = Color(0xFF1B2236)
)

private val AaiTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

private val AaiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AaiColors,
        typography = AaiTypography,
        shapes = AaiShapes,
        content = content
    )
}