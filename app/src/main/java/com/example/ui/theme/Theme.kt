package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ArenaColorScheme = darkColorScheme(
    primary = EmeraldAction,
    onPrimary = ArenaDeepNavy,
    primaryContainer = ArenaAzureCard,
    onPrimaryContainer = CrispWhite,
    secondary = ArenaBrightCyan,
    onSecondary = ArenaDeepNavy,
    secondaryContainer = ArenaSapphire,
    onSecondaryContainer = SoftIceBlue,
    tertiary = SunburstGold,
    onTertiary = ArenaDeepNavy,
    tertiaryContainer = AmethystDark,
    onTertiaryContainer = CrispWhite,
    background = ArenaDeepNavy,
    onBackground = CrispWhite,
    surface = ArenaRoyalBlue,
    onSurface = CrispWhite,
    surfaceVariant = ArenaSapphire,
    onSurfaceVariant = SoftIceBlue,
    error = CoralStrike,
    onError = CrispWhite,
    outline = MutedSlateBlue
)

private val ArenaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SnakeLegendsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArenaColorScheme,
        typography = SnakeLegendsTypography,
        shapes = ArenaShapes,
        content = content
    )
}
