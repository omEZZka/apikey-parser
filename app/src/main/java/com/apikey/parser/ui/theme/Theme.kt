package com.apikey.parser.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class ThemeEntry(val name: String, val scheme: ColorScheme)

private fun cs(primary: Long, secondary: Long, tertiary: Long, dark: Boolean): ColorScheme {
    return if (dark) darkColorScheme(
        primary = Color(primary), secondary = Color(secondary), tertiary = Color(tertiary)
    ) else lightColorScheme(
        primary = Color(primary), secondary = Color(secondary), tertiary = Color(tertiary)
    )
}

val Themes: List<ThemeEntry> = listOf(
    ThemeEntry("Dark Blue", cs(0xFF90CAF9,0xFFCE93D8,0xFF80CBC4,true)),
    ThemeEntry("Dark Green", cs(0xFF81C784,0xFFA5D6A7,0xFF4DB6AC,true)),
    ThemeEntry("Dark Red", cs(0xFFE57373,0xFFF48FB1,0xFFFF8A65,true)),
    ThemeEntry("Dark Purple", cs(0xFFBA68C8,0xFF9575CD,0xFF7986CB,true)),
    ThemeEntry("Dark Orange", cs(0xFFFFB74D,0xFFFFD54F,0xFFFF8A65,true)),
    ThemeEntry("Dark Teal", cs(0xFF4DD0E1,0xFF80DEEA,0xFF26C6DA,true)),
    ThemeEntry("Dark Pink", cs(0xFFF48FB1,0xFFCE93D8,0xFFF06292,true)),
    ThemeEntry("Dark Cyan", cs(0xFF4FC3F7,0xFF81D4FA,0xFF29B6F6,true)),
    ThemeEntry("Dark Amber", cs(0xFFFFD54F,0xFFFFE082,0xFFFFCA28,true)),
    ThemeEntry("Dark Lime", cs(0xFFDCE775,0xFFE6EE9C,0xFFCDDC39,true)),
    ThemeEntry("Dark Indigo", cs(0xFF7986CB,0xFF9FA8DA,0xFF5C6BC0,true)),
    ThemeEntry("Dark Brown", cs(0xFFA1887F,0xFFBCAAA4,0xFF8D6E63,true)),
    ThemeEntry("Dark Gray", cs(0xFFBDBDBD,0xFFE0E0E0,0xFF9E9E9E,true)),
    ThemeEntry("Dark Yellow", cs(0xFFFFF176,0xFFFFF59D,0xFFFFEE58,true)),
    ThemeEntry("Dark Violet", cs(0xFF9C27B0,0xFFBA68C8,0xFF7B1FA2,true)),
    ThemeEntry("Light Blue", cs(0xFF1976D2,0xFF7B1FA2,0xFF00796B,false)),
    ThemeEntry("Light Green", cs(0xFF388E3C,0xFF689F38,0xFF00796B,false)),
    ThemeEntry("Light Red", cs(0xFFD32F2F,0xFFC2185B,0xFFE64A19,false)),
    ThemeEntry("Light Purple", cs(0xFF7B1FA2,0xFF512DA8,0xFF303F9F,false)),
    ThemeEntry("Light Orange", cs(0xFFF57C00,0xFFFBC02D,0xFFE64A19,false)),
    ThemeEntry("Light Teal", cs(0xFF00796B,0xFF0097A7,0xFF009688,false)),
    ThemeEntry("Light Pink", cs(0xFFC2185B,0xFF7B1FA2,0xFFE91E63,false)),
    ThemeEntry("Light Cyan", cs(0xFF0288D1,0xFF039BE5,0xFF0097A7,false)),
    ThemeEntry("Light Amber", cs(0xFFFBC02D,0xFFFFA000,0xFFFF8F00,false)),
    ThemeEntry("Light Lime", cs(0xFFAFB42B,0xFFC0CA33,0xFF9E9D24,false)),
    ThemeEntry("Light Indigo", cs(0xFF303F9F,0xFF3949AB,0xFF283593,false)),
    ThemeEntry("Light Brown", cs(0xFF5D4037,0xFF6D4C41,0xFF4E342E,false)),
    ThemeEntry("Light Gray", cs(0xFF616161,0xFF757575,0xFF424242,false)),
    ThemeEntry("Light Yellow", cs(0xFFFBC02D,0xFFFFD600,0xFFFFAB00,false)),
    ThemeEntry("Light Violet", cs(0xFF6A1B9A,0xFF8E24AA,0xFF4A148C,false)),
    ThemeEntry("Ocean", cs(0xFF0288D1,0xFF26C6DA,0xFF00ACC1,true)),
    ThemeEntry("Sunset", cs(0xFFFF7043,0xFFFFCA28,0xFFEC407A,true))
)

@Composable
fun ApiKeyParserTheme(themeIndex: Int, content: @Composable () -> Unit) {
    val entry = Themes.getOrElse(themeIndex) { Themes.first() }
    MaterialTheme(colorScheme = entry.scheme, content = content)
}
