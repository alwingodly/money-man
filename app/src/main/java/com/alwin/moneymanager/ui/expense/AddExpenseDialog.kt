package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.ui.common.DateField

@Composable
fun AddExpenseDialog(
    categoryLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, dateMillis: Long, isCreditCard: Boolean) -> Unit,
    existing: Expense? = null,
) {
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var dateMillis by remember { mutableStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var isCreditCard by remember { mutableStateOf(existing?.isCreditCard ?: false) }

    val amountValue = amount.toDoubleOrNull()
    val isValid = amountValue != null && amountValue > 0
    val isEditing = existing != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit expense" else "Add $categoryLabel expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(
                    label = "Date",
                    dateMillis = dateMillis,
                    onDateSelected = { dateMillis = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Paid by credit card")
                    Switch(checked = isCreditCard, onCheckedChange = { isCreditCard = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onConfirm(amountValue!!, note.trim(), dateMillis, isCreditCard) },
            ) { Text(if (isEditing) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
