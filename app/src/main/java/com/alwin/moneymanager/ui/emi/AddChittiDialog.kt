package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.alwin.moneymanager.data.local.entity.ChittiFrequency
import java.time.Instant

data class ChittiForm(
    val name: String,
    val totalMonths: Int,
    val startDateMillis: Long,
    val notes: String,
    val frequency: ChittiFrequency,
)

@Composable
fun AddChittiDialog(
    onDismiss: () -> Unit,
    onConfirm: (ChittiForm) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var totalMonths by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(ChittiFrequency.MONTHLY) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    
    val isValid = name.isNotBlank() && totalMonths.toIntOrNull() != null && totalMonths.toInt() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chitti") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Chitti Name") },
                    placeholder = { Text("e.g., Office Chitti, Friends Group") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = totalMonths,
                    onValueChange = { totalMonths = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Months") },
                    placeholder = { Text("e.g., 30") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                
                Spacer(Modifier.height(12.dp))
                
                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = !frequencyExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Payment Frequency") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false },
                    ) {
                        ChittiFrequency.entries.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    frequency = freq
                                    frequencyExpanded = false
                                },
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Any additional details...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            ChittiForm(
                                name = name.trim(),
                                totalMonths = totalMonths.toInt(),
                                startDateMillis = Instant.now().toEpochMilli(),
                                notes = notes.trim(),
                                frequency = frequency,
                            )
                        )
                    }
                },
                enabled = isValid,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
