package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AerospaceColorScheme = darkColorScheme(
    primary = AccentIce,
    onPrimary = SpaceBackground,
    primaryContainer = SpaceSurfaceVariant,
    onPrimaryContainer = AccentIce,
    secondary = AccentGold,
    onSecondary = SpaceBackground,
    secondaryContainer = SpaceSurfaceVariant,
    onSecondaryContainer = AccentGold,
    tertiary = AccentEmerald,
    onTertiary = SpaceBackground,
    background = SpaceBackground,
    onBackground = TextPrimary,
    surface = SpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SpaceCardBorder,
    error = AccentRose
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AerospaceColorScheme,
        typography = Typography,
        content = content
    )
}
