package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PinkWaveColorScheme = darkColorScheme(
    primary = NeonPink,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepPinkMuted,
    onPrimaryContainer = PastelPink,
    secondary = SoftPink,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF331525),
    onSecondaryContainer = SoftPink,
    tertiary = WavePurple,
    onTertiary = Color.White,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ObsidianSurfaceCard,
    outline = GlassBorderNeon,
    outlineVariant = GlassBorder
)

@Composable
fun PinkWaveTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PinkWaveColorScheme,
        typography = Typography,
        content = content
    )
}
