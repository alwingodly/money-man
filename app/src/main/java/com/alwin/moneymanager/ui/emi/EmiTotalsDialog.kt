package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alwin.moneymanager.data.repository.EmiPeriodTotals
import com.alwin.moneymanager.util.formatCurrency
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EmiTotalsDialog(totals: EmiPeriodTotals, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EMI totals") },
        text = {
            if (totals.months.isEmpty()) {
                Text("No EMIs yet.")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SectionLabel("By year")
                    totals.years.forEach { TotalRow(it.year.toString(), it.amount) }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    SectionLabel("By month")
                    totals.months.forEach { TotalRow(monthLabel(it.year, it.month), it.amount) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun TotalRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun monthLabel(year: Int, month: Int): String =
    "${Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())} $year"
