package com.personalfinance.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val FinanceColorScheme = lightColorScheme(
    primary = FinancePalette.primary,
    onPrimary = FinancePalette.surface,
    primaryContainer = FinancePalette.background,
    onPrimaryContainer = FinancePalette.mainText,
    secondary = FinancePalette.accent,
    onSecondary = FinancePalette.surface,
    secondaryContainer = FinancePalette.accentContainer,
    onSecondaryContainer = FinancePalette.mainText,
    tertiary = FinancePalette.accent,
    onTertiary = FinancePalette.surface,
    tertiaryContainer = FinancePalette.accentContainer,
    onTertiaryContainer = FinancePalette.mainText,
    background = FinancePalette.background,
    onBackground = FinancePalette.mainText,
    surface = FinancePalette.surface,
    onSurface = FinancePalette.mainText,
    surfaceVariant = FinancePalette.background,
    surfaceDim = FinancePalette.background,
    surfaceBright = FinancePalette.surface,
    surfaceContainerLowest = FinancePalette.surface,
    surfaceContainerLow = FinancePalette.surface,
    surfaceContainer = FinancePalette.surface,
    surfaceContainerHigh = FinancePalette.surface,
    surfaceContainerHighest = FinancePalette.background,
    surfaceTint = FinancePalette.primary,
    inverseSurface = FinancePalette.mainText,
    inverseOnSurface = FinancePalette.surface,
    inversePrimary = FinancePalette.background,
    outline = FinancePalette.supportingText,
    outlineVariant = FinancePalette.supportingText,
    onSurfaceVariant = FinancePalette.supportingText,
    error = FinancePalette.expense,
    onError = FinancePalette.surface,
)

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FinanceColorScheme, content = content)
}
