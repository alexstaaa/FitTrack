package com.fittrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = OnAccent,
    primaryContainer = AccentOrangeDim,
    onPrimaryContainer = AccentOrange,
    secondary = TextSecondaryDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = BackgroundDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerLow = SurfaceDark,
    outline = OutlineDark,
    outlineVariant = SurfaceVariantDark,
    error = androidx.compose.ui.graphics.Color(0xFFFF5449),
)

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = OnAccent,
    background = BackgroundLight,
    surface = SurfaceLight,
)

/**
 * FitTrack brand theme. The design language is dark-first (deep black
 * surfaces, orange accent), so dark mode is the default regardless of the
 * system setting; pass darkTheme = isSystemInDarkTheme() to follow the
 * system instead. Dynamic (wallpaper) color is off so brand colors hold.
 */
@Composable
fun FitTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = FitTrackTypography,
        content = content,
    )
}
