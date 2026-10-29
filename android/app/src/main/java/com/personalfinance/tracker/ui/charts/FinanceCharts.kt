package com.personalfinance.tracker.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.personalfinance.tracker.R
import com.personalfinance.tracker.ui.theme.FinancePalette
import java.math.BigDecimal
import java.text.NumberFormat

data class FinanceBar(val label: String, val valueMinorUnits: Long, val color: Color)

data class FinanceBarGroup(val label: String, val bars: List<FinanceBar>)

data class CategoryBreakdownItem(val label: String, val valueMinorUnits: Long, val share: Float, val iconKey: String = "other")

/** Scales nonnegative chart magnitudes without integer multiplication or division by zero. */
fun normalizedBarFraction(value: Long, maximum: Long): Float =
    if (value <= 0L || maximum <= 0L) 0f
    else (value.toDouble() / maximum.toDouble()).coerceIn(0.0, 1.0).toFloat()

object FinanceCharts {
    @Composable
    fun GroupedColumnChart(groups: List<FinanceBarGroup>, modifier: Modifier = Modifier, barWidth: Dp = 80.dp) {
        val maximum = groups.flatMap { it.bars }.maxOfOrNull { it.valueMinorUnits } ?: 0L
        val chartDescription = stringResource(R.string.chart_income_expense_comparison)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = chartDescription
                    isTraversalGroup = true
                }
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            groups.forEach { group ->
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = group.label
                        isTraversalGroup = true
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(group.label, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.bars.forEach { bar ->
                            val amount = formatChartAmount(bar.valueMinorUnits)
                            val description = stringResource(R.string.chart_value_description, group.label, bar.label, amount)
                            Column(
                                modifier = Modifier.width(barWidth).clearAndSetSemantics { contentDescription = description },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(amount, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                                Box(Modifier.height(128.dp).width(32.dp), contentAlignment = Alignment.BottomCenter) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .height(128.dp * normalizedBarFraction(bar.valueMinorUnits, maximum))
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(bar.color),
                                    )
                                }
                                Text(bar.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CategoryBreakdownBars(items: List<CategoryBreakdownItem>, modifier: Modifier = Modifier) {
        val maximum = items.maxOfOrNull { it.valueMinorUnits } ?: 0L
        val chartDescription = stringResource(R.string.chart_category_breakdown)
        val locale = LocalConfiguration.current.locales[0]
        val percentFormat = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }
        Column(
            modifier = modifier.fillMaxWidth().semantics {
                contentDescription = chartDescription
                isTraversalGroup = true
            },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items.forEach { item ->
                val amount = formatChartAmount(item.valueMinorUnits)
                val share = percentFormat.format(if (item.share.isFinite()) item.share.coerceIn(0f, 1f) else 0f)
                val shareLabel = stringResource(R.string.chart_category_share, share)
                val description = stringResource(R.string.chart_category_description, item.label, amount, shareLabel)
                Column(
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.personalfinance.tracker.ui.categories.CategoryIcon(item.iconKey)
                        Text(item.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(amount, style = MaterialTheme.typography.bodyMedium)
                    }
                    LinearProgressIndicator(
                        progress = { normalizedBarFraction(item.valueMinorUnits, maximum) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = FinancePalette.accent,
                        trackColor = FinancePalette.accent.copy(alpha = 0.12f),
                    )
                    Text(shareLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatChartAmount(minorUnits: Long): String =
    BigDecimal.valueOf(minorUnits, 2).setScale(2).toPlainString()
