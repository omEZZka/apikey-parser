package com.aai.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0D1117)
val SurfaceDark = Color(0xFF161B22)
val SurfaceVariantDark = Color(0xFF21262D)
val AccentBlue = Color(0xFF58A6FF)
val AccentGreen = Color(0xFF3FB950)
val AccentRed = Color(0xFFF85149)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)

private val AaiColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1F3A5F),
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = Color(0xFF0D1117),
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = Color.White,
    outline = Color(0xFF30363D)
)

@Composable
fun AaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AaiColors,
        content = content
    )
}