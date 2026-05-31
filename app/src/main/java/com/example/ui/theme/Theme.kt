package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define core colors for our distinctive Cosmic Amber & Sepia vibe
val PrimaryAmber = Color(0xFFD97706) // Beautiful Gold/Amber accent
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryDark = Color(0xFFFBBF24)

// Neutral Colors
val LightCreamBg = Color(0xFFFAF6EE) // Safe warm cream
val DarkCharcoalBg = Color(0xFF121212) // Charcoal dark space
val SepiaBg = Color(0xFFF4ECD8)      // Vintage paper/Amber sepia
val SepiaText = Color(0xFF4A3B32)    // Warm brown text for eyes

// High Contrast Colors
val HighContrastBg = Color(0xFF000000)
val HighContrastText = Color(0xFFFFFFFF)
val HighContrastPrimary = Color(0xFFFFFF00) // Neon Yellow

val LightColorScheme = lightColorScheme(
    primary = PrimaryAmber,
    onPrimary = OnPrimaryLight,
    background = LightCreamBg,
    surface = Color(0xFFFFFDF9),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    secondary = Color(0xFF6B7280),
    outline = Color(0xFFE2E8F0)
)

val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF451A03),
    background = DarkCharcoalBg,
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    secondary = Color(0xFF9CA3AF),
    outline = Color(0xFF334155)
)

val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF8B5A2B), // Chocolate brown primary
    onPrimary = Color(0xFFFDFBF7),
    background = SepiaBg,
    surface = Color(0xFFEFE6CF),
    onBackground = SepiaText,
    onSurface = SepiaText,
    secondary = Color(0xFF705335),
    outline = Color(0xFFDFD2B4)
)

val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastPrimary,
    onPrimary = Color(0xFF000000),
    background = HighContrastBg,
    surface = Color(0xFF121212),
    onBackground = HighContrastText,
    onSurface = HighContrastText,
    secondary = HighContrastPrimary,
    outline = HighContrastText
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun ReaderSettingsTheme(
    themeSetting: String, // "system", "light", "dark", "sepia", "high_contrast"
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when (themeSetting) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "sepia" -> SepiaColorScheme
        "high_contrast" -> HighContrastColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
