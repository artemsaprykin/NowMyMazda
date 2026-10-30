package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SoulRedLight,
    onPrimary = Color.White,
    primaryContainer = SoulRedDark,
    onPrimaryContainer = Color.White,
    secondary = PlatinumSilver,
    onSecondary = CabinBackground,
    secondaryContainer = CabinSurfaceElevated,
    onSecondaryContainer = PlatinumSilver,
    tertiary = ElectricCyan,
    onTertiary = CabinBackground,
    background = CabinBackground,
    onBackground = PlatinumSilver,
    surface = CabinSurface,
    onSurface = PlatinumSilver,
    surfaceVariant = CabinSurfaceElevated,
    onSurfaceVariant = MetallicGray,
    outline = CabinBorder
)

private val LightColorScheme = lightColorScheme(
    primary = SoulRedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = SoulRedDark,
    secondary = Color(0xFF2C3238),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E7ED),
    onSecondaryContainer = Color(0xFF141920),
    tertiary = Color(0xFF00667D),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek automotive dark cabin theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
