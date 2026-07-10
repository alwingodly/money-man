package com.alwin.moneymanager.ui.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.window.DialogProperties
import com.alwin.moneymanager.ui.common.DateField
import com.alwin.moneymanager.util.currentCurrency

data class DebtFormResult(
    val personName: String,
    val isGiven: Boolean,
    val originalAmount: Double,
    val note: String,
    val dueDateMillis: Long?,
    val notificationEnabled: Boolean,
    /** When the money was given/taken (the first entry's date). Only meaningful when adding. */
    val transactionDateMillis: Long,
)

@Composable
fun DebtFormDialog(
    title: String,
    initialPersonName: String = "",
    initialIsGiven: Boolean = true,
    initialAmount: String = "",
    initialNote: String = "",
    initialDueDateMillis: Long? = null,
    initialNotificationEnabled: Boolean = false,
    existingNames: List<String> = emptyList(),
    // Editing an existing account has no single "amount" (it's a running ledger), so the amount +
    // gave/got chooser are hidden then and only the person/note/due-date are editable.
    showAmount: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (DebtFormResult) -> Unit,
) {
    var personName by remember { mutableStateOf(initialPersonName) }
    var isGiven by remember { mutableStateOf(initialIsGiven) }
    var amount by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf(initialNote) }
    var transactionDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasDueDate by remember { mutableStateOf(initialDueDateMillis != null) }
    var dueDateMillis by remember { mutableStateOf(initialDueDateMillis ?: System.currentTimeMillis()) }
    var notificationEnabled by remember { mutableStateOf(initialNotificationEnabled) }

    val amountValue = amount.toDoubleOrNull()
    val amountOk = !showAmount || (amountValue != null && amountValue > 0)
    val isValid = personName.isNotBlank() && amountOk

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (showAmount) {
                    Text(
                        "Who owes whom?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = isGiven,
                            onClick = { isGiven = true },
                            label = { Text("They owe me") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                        FilterChip(
                            selected = !isGiven,
                            onClick = { isGiven = false },
                            label = { Text("I owe them") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                // Autocomplete over already-used names so the same person is reused (one "Ammu",
                // not two) with no contacts permission. A filtered, scrollable dropdown instead of
                // a chip row so it scales to a long list of people — typing narrows it.
                var nameFieldExpanded by remember { mutableStateOf(false) }
                val nameSuggestions = remember(personName, existingNames) {
                    val typed = personName.trim().lowercase()
                    existingNames.filter {
                        it.lowercase() != typed && (typed.isEmpty() || it.lowercase().contains(typed))
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = nameFieldExpanded && nameSuggestions.isNotEmpty(),
                    onExpandedChange = { nameFieldExpanded = it },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = {
                            personName = it
                            nameFieldExpanded = true
                        },
                        label = { Text("Person's name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = nameFieldExpanded && nameSuggestions.isNotEmpty(),
                        onDismissRequest = { nameFieldExpanded = false },
                    ) {
                        nameSuggestions.take(15).forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    personName = name
                                    nameFieldExpanded = false
                                },
                            )
                        }
                    }
                }
                if (showAmount) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        prefix = { Text(currentCurrency.symbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    DateField(
                        label = if (isGiven) "Date given" else "Date received",
                        dateMillis = transactionDateMillis,
                        onDateSelected = { transactionDateMillis = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
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
                    Text("Set a due date")
                    Switch(
                        checked = hasDueDate,
                        onCheckedChange = {
                            hasDueDate = it
                            if (!it) notificationEnabled = false
                        },
                    )
                }
                if (hasDueDate) {
                    DateField(
                        label = "Due date",
                        dateMillis = dueDateMillis,
                        onDateSelected = { dueDateMillis = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Remind me on this date")
                        Switch(checked = notificationEnabled, onCheckedChange = { notificationEnabled = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(
                        DebtFormResult(
                            personName = personName.trim(),
                            isGiven = isGiven,
                            originalAmount = amountValue ?: 0.0,
                            note = note.trim(),
                            dueDateMillis = if (hasDueDate) dueDateMillis else null,
                            notificationEnabled = hasDueDate && notificationEnabled,
                            transactionDateMillis = transactionDateMillis,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
