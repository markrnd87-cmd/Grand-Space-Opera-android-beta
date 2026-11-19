package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpaceOperaColorScheme = darkColorScheme(
    primary = VerdantCyan,
    onPrimary = DeepSpaceBg,
    primaryContainer = CardHighlightBg,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldBio,
    onSecondary = DeepSpaceBg,
    secondaryContainer = CardNavyBg,
    onSecondaryContainer = EmeraldBio,
    tertiary = SolarGold,
    onTertiary = DeepSpaceBg,
    error = CrimsonAlert,
    onError = DeepSpaceBg,
    background = DeepSpaceBg,
    onBackground = TextPrimary,
    surface = PanelDarkBg,
    onSurface = TextPrimary,
    surfaceVariant = CardNavyBg,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle
)

@Composable
fun GrandSpaceOperaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpaceOperaColorScheme,
        typography = Typography,
        content = content
    )
}
