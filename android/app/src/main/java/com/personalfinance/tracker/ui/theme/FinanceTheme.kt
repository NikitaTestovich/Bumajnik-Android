package com.personalfinance.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable

val LocalAppTheme = staticCompositionLocalOf { AppThemeRegistry.default }
fun themeColorScheme(theme: AppTheme): ColorScheme {
    val dark = theme.dark
    val ink = if (dark) Color(0xFFF3F4F5) else if (theme.id == 8) Color(0xFF352453) else Color(0xFF253346)
    val muted = if (dark) Color(0xFFBCC4C9) else if (theme.id == 8) Color(0xFF655A78) else Color(0xFF5C6268)
    val background = when(theme.id) { 8 -> Color(0xFFF2EDFF); 6,9 -> Color(0xFF181818); else -> Color(0xFFF5F3EE) }
    val surface = if (dark) Color(0xFF272727) else if(theme.id == 8) Color(0xFFFFFCFF) else Color(0xFFFFFEFA)
    val primary = when(theme.id) { 6 -> Color(0xFF9AAEFF); 8 -> Color(0xFF6744CB); 9 -> Color(0xFF68DCCD); else -> Color(0xFF445C77) }
    val secondary = when(theme.id) { 6 -> Color(0xFFA7EFB5); 8 -> Color(0xFF705516); 9 -> Color(0xFFFFC078); else -> Color(0xFF946536) }
    val container = when(theme.id) { 6 -> Color(0xFF313131); 8 -> Color(0xFFE5DCFF); 9 -> Color(0xFF313131); else -> surface }
    val secondaryContainer = if(dark) Color(0xFF353C43) else if(theme.id == 8) Color(0xFFEADF7E) else Color(0xFFEAE3D6)
    val scheme = if(dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(primary = primary, onPrimary = if(dark) Color(0xFF181818) else Color.White,
        primaryContainer = container, onPrimaryContainer = ink,
        secondary = secondary, onSecondary = if(dark) Color(0xFF181818) else Color.White,
        secondaryContainer = secondaryContainer, onSecondaryContainer = ink,
        tertiary = secondary, onTertiary = if(dark) Color(0xFF181818) else Color.White,
        tertiaryContainer = secondaryContainer, onTertiaryContainer = ink,
        background = background, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = container, onSurfaceVariant = muted, surfaceDim = background, surfaceBright = surface,
        surfaceContainerLowest = background, surfaceContainerLow = if(theme.id == 8) container else surface,
        surfaceContainer = container, surfaceContainerHigh = container, surfaceContainerHighest = container,
        surfaceTint = primary, outline = muted, outlineVariant = if(dark) Color(0xFF555555) else Color(0xFFCEC8D2),
        error = financeColors(theme).expense, onError = if(dark) Color(0xFF181818) else Color.White,
        errorContainer = if(dark) Color(0xFF5A2428) else Color(0xFFFFDAD6),
        onErrorContainer = if(dark) Color(0xFFFFDAD6) else Color(0xFF410005),
        inverseSurface = ink, inverseOnSurface = background, inversePrimary = primary)
}
@Composable
fun FinanceTheme(theme: AppTheme = LocalAppTheme.current, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = themeColorScheme(theme), shapes = themeShapes(theme), typography = themeTypography(theme), content = content)
    }
}
