package com.personalfinance.tracker.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReportExpenseDetail(date: String, amount: String, comment: String, originalAmount: String?) {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(date, modifier = Modifier.weight(1f))
            Text(amount, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        if (comment.isNotBlank()) Text(comment, Modifier.testTag("expense_detail_comment"))
        if (originalAmount != null) Text(originalAmount, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.padding(top = 8.dp).testTag("expense_detail_separator"))
    }
}
