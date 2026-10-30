package com.personalfinance.tracker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun themeShapes(theme: AppTheme): Shapes {
    val radius = when(theme.id) { 2 -> 8; 8 -> 24; 9 -> 16; else -> 16 }
    return Shapes(extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape((radius/2).dp), medium = RoundedCornerShape(radius.dp), large = RoundedCornerShape(radius.dp), extraLarge = RoundedCornerShape((radius+8).dp))
}
fun themeTypography(theme: AppTheme): Typography {
    val base = Typography()
    return if(theme.id == 2) base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    ) else base.copy(headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold), displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold))
}

/** Shared card geometry/borders keep forms, family, dialogs and lists in the same design. */
@Composable
fun FinanceCard(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurface),
    border: BorderStroke? = if(LocalAppTheme.current.id in listOf(2,6)) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
}

@Composable
private fun actionShape(): Shape = RoundedCornerShape(when(LocalAppTheme.current.id) { 8 -> 16.dp; 6 -> 12.dp; else -> 8.dp })

@Composable
fun FinanceButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Button(onClick, modifier, enabled = enabled, shape = actionShape(), colors = colors, content = content)
}

@Composable
fun FinanceOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    OutlinedButton(onClick, modifier, enabled = enabled, shape = actionShape(), colors = colors, content = content)
}
