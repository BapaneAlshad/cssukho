package com.sukhoramen.cs.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.sukhoramen.cs.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1769D2), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F1FF), onPrimaryContainer = Color(0xFF18559E),
    secondary = Color(0xFF62758D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF3FC), onSecondaryContainer = Color(0xFF425C7A),
    background = Color(0xFFF1F6FF), onBackground = Color(0xFF172B4D),
    surface = Color.White, onSurface = Color(0xFF172B4D),
    surfaceVariant = Color(0xFFEBF2FC), onSurfaceVariant = Color(0xFF61748D),
    outline = Color(0xFF8191A6), outlineVariant = Color(0xFFDFE9F7),
    error = Color(0xFFB43B45), errorContainer = Color(0xFFFFEEF0), onErrorContainer = Color(0xFF912B35),
    tertiary = Color(0xFF26765E), tertiaryContainer = Color(0xFFE7F6EF), onTertiaryContainer = Color(0xFF1A694D),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF86BDFF), onPrimary = Color(0xFF082C58),
    primaryContainer = Color(0xFF213F63), onPrimaryContainer = Color(0xFFC8E2FF),
    secondary = Color(0xFFA3B7D0), onSecondary = Color(0xFF13283F),
    secondaryContainer = Color(0xFF22354C), onSecondaryContainer = Color(0xFFD7E5F6),
    background = Color(0xFF0C1728), onBackground = Color(0xFFECF4FF),
    surface = Color(0xFF14243A), onSurface = Color(0xFFECF4FF),
    surfaceVariant = Color(0xFF20324A), onSurfaceVariant = Color(0xFFADBDD2),
    outline = Color(0xFF7B90AD), outlineVariant = Color(0xFF2B405B),
    error = Color(0xFFFFA5AF), errorContainer = Color(0xFF442630), onErrorContainer = Color(0xFFFFCFD5),
    tertiary = Color(0xFF8CD6B6), tertiaryContainer = Color(0xFF193F36), onTertiaryContainer = Color(0xFFAAE6CB),
)

private val SukhoTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun SukhoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = SukhoTypography, content = content)
}

