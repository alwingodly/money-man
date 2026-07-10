package com.alwin.moneymanager.ui.saving

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.alwin.moneymanager.util.currentCurrency

data class SavingFormResult(
    val name: String,
    val targetAmount: Double?,
    val note: String,
)

@Composable
fun SavingFormDialog(
    title: String,
    initialName: String = "",
    initialTarget: String = "",
    initialNote: String = "",
    onDismiss: () -> Unit,
    onConfirm: (SavingFormResult) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var target by remember { mutableStateOf(initialTarget) }
    var note by remember { mutableStateOf(initialNote) }

    val targetValue = target.toDoubleOrNull()
    val targetOk = target.isBlank() || (targetValue != null && targetValue > 0)
    val isValid = name.isNotBlank() && targetOk

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What are you saving for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Goal amount (optional)") },
                    supportingText = { Text("Leave blank for an open-ended savings pot") },
                    prefix = { Text(currentCurrency.symbol) },
                    singleLine = true,
                    isError = !targetOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(
                        SavingFormResult(
                            name = name.trim(),
                            targetAmount = if (target.isBlank()) null else targetValue,
                            note = note.trim(),
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
