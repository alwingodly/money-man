package com.alwin.moneymanager.ui.debt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.alwin.moneymanager.ui.common.DateField
import com.alwin.moneymanager.util.currentCurrency
import com.alwin.moneymanager.util.formatCurrency

/**
 * A single-amount entry dialog, reused for both ledger actions. [maxAmount] caps the input when set
 * (a repayment can't exceed the outstanding balance); pass null for "gave more" (no ceiling).
 * Includes a date so the user can record when the money actually changed hands, not just today.
 */
@Composable
fun DebtAmountDialog(
    title: String,
    maxAmount: Double? = null,
    prefillMax: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, dateMillis: Long) -> Unit,
) {
    var amount by remember {
        mutableStateOf(if (prefillMax && maxAmount != null && maxAmount > 0) trimZeros(maxAmount) else "")
    }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val amountValue = amount.toDoubleOrNull()
    // Small epsilon so paying the exact outstanding balance isn't rejected by float drift.
    val exceedsMax = maxAmount != null && amountValue != null && amountValue > maxAmount + 0.001
    val isValid = amountValue != null && amountValue > 0 && !exceedsMax

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = {
            Column {
                if (maxAmount != null) {
                    Text(
                        "Outstanding: ${formatCurrency(maxAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(currentCurrency.symbol) },
                    singleLine = true,
                    isError = exceedsMax,
                    supportingText = if (exceedsMax) {
                        { Text("Can't be more than the ${formatCurrency(maxAmount!!)} outstanding") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                DateField(
                    label = "Date",
                    dateMillis = dateMillis,
                    onDateSelected = { dateMillis = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = { onConfirm(amountValue!!, dateMillis) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** "500.0" -> "500", "500.5" -> "500.5" so a prefilled amount reads cleanly. */
private fun trimZeros(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
