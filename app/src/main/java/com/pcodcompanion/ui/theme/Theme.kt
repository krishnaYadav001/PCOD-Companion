package com.pcodcompanion.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PastelPinkDark,
    onPrimary = Color.White,
    primaryContainer = PastelPinkLight,
    onPrimaryContainer = OnSurfaceLight,
    secondary = PastelLavenderDark,
    onSecondary = Color.White,
    secondaryContainer = PastelLavenderLight,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = PastelPeachDark,
    onTertiary = Color.White,
    tertiaryContainer = PastelPeachLight,
    onTertiaryContainer = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = PastelPinkLight,
    onSurfaceVariant = OnSurfaceLight,
    outline = PastelLavenderDark
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF4A2D3C),       // softer plum-pink container
    onPrimaryContainer = OnSurfaceDark,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF3A304E),     // muted lavender container
    onSecondaryContainer = OnSurfaceDark,
    tertiary = DarkTertiary,
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF4D3A2E),      // muted peach container
    onTertiaryContainer = OnSurfaceDark,
    background = DarkBackground,
    onBackground = OnSurfaceDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceDark,
    outline = DarkSecondary
)

@Composable
fun PCODCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
