package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val title: String) {
    DARK("Dark Cabin"),
    LIGHT("Light Ceramic"),
    SYSTEM("System Default")
}

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val radarBackground: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background = Color(0xFF0C0F12),
    surface = Color(0xFF141920),
    surfaceElevated = Color(0xFF1D232C),
    border = Color(0xFF2E3846),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    radarBackground = Color(0xFF0A0D11),
    isDark = true
)

val LightAppColors = AppColors(
    background = Color(0xFFF3F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE9EEF4),
    border = Color(0xFFD6DFE8),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    radarBackground = Color(0xFF1A222C),
    isDark = false
)

val LocalAppThemeColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemeColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = SoulRedLight,
    onPrimary = Color.White,
    primaryContainer = SoulRedDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFF1F5F9),
    onSecondary = Color(0xFF0C0F12),
    secondaryContainer = Color(0xFF1D232C),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF0C0F12),
    background = Color(0xFF0C0F12),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF141920),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1D232C),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2E3846)
)

private val LightColorScheme = lightColorScheme(
    primary = SoulRedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = SoulRedDark,
    secondary = Color(0xFF1E293B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EEF4),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFD6DFE8)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val appThemeColors = if (isDark) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppThemeColors provides appThemeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
