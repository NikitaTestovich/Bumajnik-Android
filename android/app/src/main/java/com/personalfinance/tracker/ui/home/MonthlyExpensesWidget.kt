package com.personalfinance.tracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import com.personalfinance.tracker.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.ui.categories.CategoryIcon
import com.personalfinance.tracker.ui.charts.ExpenseSlice
import java.math.BigDecimal
import kotlin.math.*

private val sliceColors = listOf(Color(0xFF527B9C), Color(0xFF438D87), Color(0xFFC28C57), Color(0xFF8074A5), Color(0xFFAB657A), Color(0xFF78944E))
internal fun expenseColor(id: String?): Color = sliceColors[Math.floorMod((id ?: "uncategorized").hashCode(), sliceColors.size)]

@Composable
fun MonthlyExpensesWidget(slices: List<ExpenseSlice>, currency: String, languageTag: String, missingRates: Boolean = false, onRates: () -> Unit = {}) {
    val heading = stringResource(R.string.monthly_expenses_title)
    val totalLabel = stringResource(R.string.monthly_expenses_total)
    var selected by remember { mutableStateOf<String?>(null) }
    var hasSelection by remember { mutableStateOf(false) }
    LaunchedEffect(slices) { if (hasSelection && slices.none { it.categoryId == selected }) hasSelection = false }
    val chosen = if (hasSelection) slices.find { it.categoryId == selected } else null
    val total = slices.fold(0L) { acc, item -> Math.addExact(acc, item.minorUnits) }
    fun amount(value: Long) = "${BigDecimal.valueOf(value, 2).toPlainString()} $currency"
    Card(Modifier.fillMaxWidth().testTag("monthly_expenses")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(heading, style = MaterialTheme.typography.titleLarge)
            when {
                missingRates -> { Text(stringResource(R.string.monthly_expenses_rates)); OutlinedButton(onRates) { Text(stringResource(R.string.exchange_rate_open)) } }
                slices.isEmpty() -> Text(stringResource(R.string.monthly_expenses_empty), Modifier.testTag("expenses_empty"))
                else -> {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(250.dp).testTag("expense_ring").semantics { contentDescription = "$heading: ${amount(total)}" }.pointerInput(slices) {
                            detectTapGestures { point ->
                                val dx = point.x - size.width / 2f
                                val dy = point.y - size.height / 2f
                                val radius = minOf(size.width, size.height) / 2f
                                if (hypot(dx, dy) in radius * .54f..radius) {
                                    val fraction = ((atan2(dy, dx) * 180 / PI + 450) % 360) / 360
                                    var end = 0.0
                                    val slice = slices.firstOrNull { end += it.share; fraction < end } ?: slices.last()
                                    if (hasSelection && selected == slice.categoryId) hasSelection = false else { selected = slice.categoryId; hasSelection = true }
                                }
                            }
                        }) {
                            val width = size.minDimension * .23f
                            val inset = width / 2
                            var angle = -90f
                            slices.forEach { slice ->
                                val sweep = (slice.share * 360).toFloat()
                                drawArc(expenseColor(slice.categoryId).copy(alpha = if (chosen == null || chosen.categoryId == slice.categoryId) 1f else .25f), angle, sweep, false, Offset(inset, inset), androidx.compose.ui.geometry.Size(size.width-width, size.height-width), style = Stroke(width))
                                if (slice.share >= .075) {
                                    val radians = Math.toRadians((angle + sweep / 2).toDouble())
                                    val radius = (size.minDimension-width)/2
                                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textAlign = android.graphics.Paint.Align.CENTER; textSize = 13.dp.toPx(); isFakeBoldText = true }
                                    drawContext.canvas.nativeCanvas.drawText("${(slice.share*100).roundToInt()}%", center.x + cos(radians).toFloat()*radius, center.y + sin(radians).toFloat()*radius + paint.textSize/3, paint)
                                }
                                angle += sweep
                            }
                        }
                        Column(Modifier.width(125.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(chosen?.name ?: totalLabel, style = MaterialTheme.typography.labelLarge)
                            Text(amount(chosen?.minorUnits ?: total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("expense_center"))
                        }
                    }
                    slices.forEach { slice ->
                        val active = chosen?.categoryId == slice.categoryId && hasSelection
                        OutlinedCard(Modifier.fillMaxWidth().testTag("expense_slice_${slice.categoryId}").semantics(mergeDescendants = true) { this.selected = active; contentDescription = "${slice.name}, ${amount(slice.minorUnits)}, ${(slice.share*100).roundToInt()}%" }.clickable { if (hasSelection && selected == slice.categoryId) hasSelection = false else { selected = slice.categoryId; hasSelection = true } }, shape = RoundedCornerShape(16.dp), border = BorderStroke(if (active) 2.dp else 1.dp, if (active) expenseColor(slice.categoryId) else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { CategoryIcon(slice.iconKey, Modifier.size(24.dp)); Text(slice.name, Modifier.weight(1f)); Text("${(slice.share*100).roundToInt()}%") }
                                Text(amount(slice.minorUnits), style = MaterialTheme.typography.titleMedium)
                                LinearProgressIndicator(progress = { slice.share.toFloat() }, modifier = Modifier.fillMaxWidth(), color = expenseColor(slice.categoryId))
                            }
                        }
                    }
                }
            }
        }
    }
}
