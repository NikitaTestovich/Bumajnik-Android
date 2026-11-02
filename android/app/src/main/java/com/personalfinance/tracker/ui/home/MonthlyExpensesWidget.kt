package com.personalfinance.tracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.personalfinance.tracker.ui.theme.FinanceOutlinedButton as OutlinedButton
import com.personalfinance.tracker.ui.theme.FinanceCard as Card
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.personalfinance.tracker.ui.theme.LocalAppTheme
import com.personalfinance.tracker.ui.theme.financeColors
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import com.personalfinance.tracker.ui.categories.CategoryIcon
import com.personalfinance.tracker.ui.charts.ExpenseSlice
import com.personalfinance.tracker.ui.charts.chartColorIndices
import java.math.BigDecimal
import kotlin.math.*

@Composable
fun MonthlyExpensesWidget(slices: List<ExpenseSlice>, currency: String, languageTag: String, missingRates: Boolean = false, onRates: () -> Unit = {}) {
    val theme = LocalAppTheme.current
    val chartColors = financeColors.chart
    val visibleSlices = slices.filter { it.share > 0 }
    val assignedColors = visibleSlices.zip(chartColorIndices(visibleSlices.size, chartColors.size))
        .associate { (slice, index) -> slice.categoryId to chartColors[index] }
    fun expenseColor(id: String?) = assignedColors[id] ?: chartColors.first()
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
                                if (total > 0 && hypot(dx, dy) in radius * .54f..radius) {
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
                                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = (if (theme.dark) Color(0xFF181818) else Color.White).toArgb(); textAlign = android.graphics.Paint.Align.CENTER; textSize = 13.dp.toPx(); isFakeBoldText = true }
                                    drawContext.canvas.nativeCanvas.drawText("${(slice.share*100).roundToInt()}%", center.x + cos(radians).toFloat()*radius, center.y + sin(radians).toFloat()*radius + paint.textSize/3, paint)
                                }
                                angle += sweep
                            }
                        }
                        Column(Modifier.width(125.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(chosen?.name ?: totalLabel, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("expense_center_name"))
                            BasicText(amount(chosen?.minorUnits ?: total), style = MaterialTheme.typography.titleMedium.copy(color = LocalContentColor.current), modifier = Modifier.testTag("expense_center"), autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.titleMedium.fontSize), maxLines = 1)
                        }
                    }
                    slices.forEach { slice ->
                        val active = chosen?.categoryId == slice.categoryId && hasSelection
                        OutlinedCard(Modifier.fillMaxWidth().testTag("expense_slice_${slice.categoryId}").semantics(mergeDescendants = true) { this.selected = active; contentDescription = "${slice.name}, ${amount(slice.minorUnits)}, ${(slice.share*100).roundToInt()}%" }.clickable { if (hasSelection && selected == slice.categoryId) hasSelection = false else { selected = slice.categoryId; hasSelection = true } }, shape = RoundedCornerShape(16.dp), border = BorderStroke(if (active) 2.dp else 1.dp, if (active) expenseColor(slice.categoryId) else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { CategoryIcon(slice.iconKey, Modifier.size(24.dp)); Text(slice.name, Modifier.weight(1f)); Text("${(slice.share*100).roundToInt()}%") }
                                Text(amount(slice.minorUnits), style = MaterialTheme.typography.titleMedium)
                                LinearProgressIndicator(progress = { slice.share.toFloat() }, modifier = Modifier.fillMaxWidth().height(if (theme.id == 9) 8.dp else 4.dp), color = expenseColor(slice.categoryId), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                            }
                        }
                    }
                }
            }
        }
    }
}
