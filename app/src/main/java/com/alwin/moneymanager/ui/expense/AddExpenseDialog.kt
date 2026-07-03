package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.ui.common.DateField
import com.alwin.moneymanager.util.currentCurrency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, amount: Double, note: String, dateMillis: Long, isCreditCard: Boolean) -> Unit,
    initialCategoryId: Long? = null,
    existing: Expense? = null,
) {
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var dateMillis by remember { mutableStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var isCreditCard by remember { mutableStateOf(existing?.isCreditCard ?: false) }
    var categoryId by remember {
        mutableStateOf(existing?.categoryId ?: initialCategoryId ?: categories.firstOrNull()?.id)
    }

    val amountValue = amount.toDoubleOrNull()
    val isValid = amountValue != null && amountValue > 0 && categoryId != null
    val isEditing = existing != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit expense" else "Add expense") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Category is chosen right here in the dialog — the user no longer has to tap the
                // right chip on the screen behind before opening this.
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id == categoryId,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(currentCurrency.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
                onClick = { onConfirm(categoryId!!, amountValue!!, note.trim(), dateMillis, isCreditCard) },
            ) { Text(if (isEditing) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
