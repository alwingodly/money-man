package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alwin.moneymanager.ui.common.DateField
import com.alwin.moneymanager.util.calendarMonthsInclusive

data class EmiFormResult(
    val name: String,
    val monthlyAmount: Double,
    val totalMonths: Int,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val notes: String,
    val notificationEnabled: Boolean,
    val reminderDaysBefore: Int,
    val loanAmount: Double,
)

@Composable
fun EmiFormDialog(
    title: String,
    initialName: String = "",
    initialMonthlyAmount: String = "",
    initialStartDateMillis: Long = System.currentTimeMillis(),
    initialEndDateMillis: Long? = null,
    initialNotes: String = "",
    initialNotificationEnabled: Boolean = false,
    initialReminderDaysBefore: Int = 3,
    initialLoanAmount: String = "",
    minTotalMonths: Int = 1,
    onDismiss: () -> Unit,
    onConfirm: (EmiFormResult) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var monthlyAmount by remember { mutableStateOf(initialMonthlyAmount) }
    var loanAmount by remember { mutableStateOf(initialLoanAmount) }
    var notes by remember { mutableStateOf(initialNotes) }
    var startDateMillis by remember { mutableStateOf(initialStartDateMillis) }
    var endDateMillis by remember {
        mutableStateOf(initialEndDateMillis ?: (initialStartDateMillis + MILLIS_PER_YEAR))
    }
    var notificationEnabled by remember { mutableStateOf(initialNotificationEnabled) }
    var reminderDaysBefore by remember { mutableStateOf(initialReminderDaysBefore.toString()) }

    val amountValue = monthlyAmount.toDoubleOrNull()
    // Counted by (year, month) only — day-of-month is ignored so the total doesn't shift
    // depending on which day of the month start/end happen to fall on.
    val totalMonths = calendarMonthsInclusive(startDateMillis, endDateMillis)
    val reminderDaysValue = reminderDaysBefore.toIntOrNull()
    val loanAmountValue = loanAmount.toDoubleOrNull()
    val isLoanAmountValid = loanAmount.isBlank() || (loanAmountValue != null && loanAmountValue > 0)
    val isValid = name.isNotBlank() && amountValue != null && amountValue > 0 &&
        endDateMillis > startDateMillis && totalMonths >= minTotalMonths &&
        (!notificationEnabled || (reminderDaysValue != null && reminderDaysValue >= 0)) &&
        isLoanAmountValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = monthlyAmount,
                    onValueChange = { monthlyAmount = it },
                    label = { Text("Monthly amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = loanAmount,
                    onValueChange = { loanAmount = it },
                    label = { Text("Loan amount (optional)") },
                    supportingText = { Text("Original amount borrowed — lets us show the total interest you're paying") },
                    singleLine = true,
                    isError = !isLoanAmountValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    DateField(
                        label = "Start date",
                        dateMillis = startDateMillis,
                        onDateSelected = { startDateMillis = it },
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = "End date",
                        dateMillis = endDateMillis,
                        onDateSelected = { endDateMillis = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                }
                Text(
                    if (endDateMillis > startDateMillis) "$totalMonths months" else "End date must be after start date",
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Remind me before due date")
                    Switch(checked = notificationEnabled, onCheckedChange = { notificationEnabled = it })
                }
                if (notificationEnabled) {
                    OutlinedTextField(
                        value = reminderDaysBefore,
                        onValueChange = { reminderDaysBefore = it },
                        label = { Text("Days before due date") },
                        singleLine = true,
                        isError = reminderDaysValue == null || reminderDaysValue < 0,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(
                        EmiFormResult(
                            name = name.trim(),
                            monthlyAmount = amountValue!!,
                            totalMonths = totalMonths,
                            startDateMillis = startDateMillis,
                            endDateMillis = endDateMillis,
                            notes = notes.trim(),
                            notificationEnabled = notificationEnabled,
                            reminderDaysBefore = reminderDaysValue ?: initialReminderDaysBefore,
                            loanAmount = loanAmountValue ?: 0.0,
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

private const val MILLIS_PER_YEAR = 365L * 24 * 60 * 60 * 1000
