package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import com.alwin.moneymanager.data.local.entity.ExpenseCategory

@Composable
fun AddCategoryDialog(
    existing: ExpenseCategory? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, budgetLimit: Double?) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var budgetText by remember {
        mutableStateOf(existing?.budgetLimit?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(if (existing != null) "Edit category" else "New category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { input -> budgetText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monthly budget (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), budgetText.toDoubleOrNull()) },
            ) { Text(if (existing != null) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
