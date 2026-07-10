package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.window.DialogProperties
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiFrequency
import com.alwin.moneymanager.ui.common.DateField
import com.alwin.moneymanager.util.calendarMonthsInclusive
import com.alwin.moneymanager.util.emiEndDate
import com.alwin.moneymanager.util.offDaysMaskOf
import com.alwin.moneymanager.util.offDaysOf
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

data class EmiFormResult(
    val name: String,
    val monthlyAmount: Double,
    val totalMonths: Int,
    val startDateMillis: Long,
    val notes: String,
    val notificationEnabled: Boolean,
    val reminderDaysBefore: Int,
    val loanAmount: Double,
    val frequency: EmiFrequency,
    val offDaysMask: Int,
    val intervalDays: Int,
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
    initialFrequency: EmiFrequency = EmiFrequency.MONTHLY,
    initialTotalCount: Int? = null,
    initialOffDaysMask: Int = 0,
    initialIntervalDays: Int = 0,
    minTotalMonths: Int = 1,
    onDismiss: () -> Unit,
    onConfirm: (EmiFormResult) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var frequency by remember { mutableStateOf(initialFrequency) }
    var monthlyAmount by remember { mutableStateOf(initialMonthlyAmount) }
    var loanAmount by remember { mutableStateOf(initialLoanAmount) }
    var notes by remember { mutableStateOf(initialNotes) }
    var startDateMillis by remember { mutableStateOf(initialStartDateMillis) }
    var endDateMillis by remember {
        mutableStateOf(initialEndDateMillis ?: (initialStartDateMillis + MILLIS_PER_YEAR))
    }
    // Installment count for weekly/daily/custom (months are derived from the date range instead).
    var countText by remember { mutableStateOf(initialTotalCount?.toString() ?: "") }
    var offDays by remember { mutableStateOf(offDaysOf(initialOffDaysMask)) }
    // Days between installments, for the CUSTOM "every N days" frequency.
    var intervalText by remember { mutableStateOf(if (initialIntervalDays > 0) initialIntervalDays.toString() else "") }
    var notificationEnabled by remember { mutableStateOf(initialNotificationEnabled) }
    var reminderDaysBefore by remember { mutableStateOf(initialReminderDaysBefore.toString()) }

    val amountValue = monthlyAmount.toDoubleOrNull()
    val reminderDaysValue = reminderDaysBefore.toIntOrNull()
    val loanAmountValue = loanAmount.toDoubleOrNull()
    val isLoanAmountValid = loanAmount.isBlank() || (loanAmountValue != null && loanAmountValue > 0)

    // Total installments: months are counted (year, month)-only for MONTHLY; weekly/daily use the
    // explicit count field.
    val totalMonths = when (frequency) {
        EmiFrequency.MONTHLY -> calendarMonthsInclusive(startDateMillis, endDateMillis)
        else -> countText.toIntOrNull() ?: 0
    }
    val offDaysMask = if (frequency == EmiFrequency.DAILY) offDaysMaskOf(offDays) else 0
    val intervalValue = intervalText.toIntOrNull()
    val intervalDays = if (frequency == EmiFrequency.CUSTOM) intervalValue ?: 0 else 0

    val scheduleValid = when (frequency) {
        EmiFrequency.MONTHLY -> endDateMillis > startDateMillis && totalMonths >= minTotalMonths
        EmiFrequency.CUSTOM -> totalMonths >= minTotalMonths && intervalValue != null && intervalValue > 0
        else -> totalMonths >= minTotalMonths
    }
    val isValid = name.isNotBlank() && amountValue != null && amountValue > 0 &&
        scheduleValid &&
        (!notificationEnabled || (reminderDaysValue != null && reminderDaysValue >= 0)) &&
        isLoanAmountValid

    val unitPlural = when (frequency) {
        EmiFrequency.MONTHLY -> "months"
        EmiFrequency.WEEKLY -> "weeks"
        EmiFrequency.DAILY -> "days"
        EmiFrequency.CUSTOM -> "payments"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    EmiFrequency.entries.forEachIndexed { index, freq ->
                        SegmentedButton(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            shape = SegmentedButtonDefaults.itemShape(index, EmiFrequency.entries.size),
                        ) {
                            Text(freq.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = monthlyAmount,
                    onValueChange = { monthlyAmount = it },
                    label = {
                        Text(
                            when (frequency) {
                                EmiFrequency.MONTHLY -> "Monthly amount"
                                EmiFrequency.WEEKLY -> "Weekly amount"
                                EmiFrequency.DAILY -> "Daily amount"
                                EmiFrequency.CUSTOM -> "Amount per payment"
                            }
                        )
                    },
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

                if (frequency == EmiFrequency.MONTHLY) {
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
                } else {
                    DateField(
                        label = "Start date",
                        dateMillis = startDateMillis,
                        onDateSelected = { startDateMillis = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    if (frequency == EmiFrequency.WEEKLY) {
                        Text("Due every week on", modifier = Modifier.padding(top = 8.dp))
                        WeekdayChips(
                            selected = setOf(startWeekday(startDateMillis)),
                            onToggle = { day -> startDateMillis = adjustToWeekday(startDateMillis, day) },
                        )
                    }
                    if (frequency == EmiFrequency.CUSTOM) {
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { intervalText = it },
                            label = { Text("Repeat every … days") },
                            supportingText = { Text("e.g. 28 for every 28 days — lands on the same weekday as the start") },
                            singleLine = true,
                            isError = intervalText.isNotBlank() && (intervalValue == null || intervalValue <= 0),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = {
                            Text(
                                when (frequency) {
                                    EmiFrequency.WEEKLY -> "Number of weeks"
                                    EmiFrequency.DAILY -> "Number of days"
                                    else -> "Number of payments"
                                }
                            )
                        },
                        singleLine = true,
                        isError = countText.isNotBlank() && totalMonths < minTotalMonths,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    if (frequency == EmiFrequency.DAILY) {
                        Text("Off days (skipped)", modifier = Modifier.padding(top = 8.dp))
                        WeekdayChips(
                            selected = offDays,
                            onToggle = { day ->
                                val next = if (day in offDays) offDays - day else offDays + day
                                // Never let all seven be off — the schedule would have no working day.
                                if (next.size < DayOfWeek.entries.size) offDays = next
                            },
                        )
                    }
                    if (totalMonths >= minTotalMonths) {
                        val end = emiEndDate(
                            Emi(
                                name = "",
                                monthlyAmount = 0.0,
                                totalMonths = totalMonths,
                                startDateMillis = startDateMillis,
                                frequency = frequency,
                                offDaysMask = offDaysMask,
                                intervalDays = intervalDays,
                            ),
                            totalMonths,
                        )
                        Text(
                            "$totalMonths $unitPlural · ends ${dateFormat.format(Date(end))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

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
                            notes = notes.trim(),
                            notificationEnabled = notificationEnabled,
                            reminderDaysBefore = reminderDaysValue ?: initialReminderDaysBefore,
                            loanAmount = loanAmountValue ?: 0.0,
                            frequency = frequency,
                            offDaysMask = offDaysMask,
                            intervalDays = intervalDays,
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

/** Mon–Sun chips. [onToggle] fires with the tapped day; the caller decides single- vs multi-select. */
@Composable
private fun WeekdayChips(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
            )
        }
    }
}

private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

private fun startWeekday(millis: Long): DayOfWeek =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).dayOfWeek

/** Moves [millis] forward to the next date whose weekday is [day] (or keeps it if already that day). */
private fun adjustToWeekday(millis: Long, day: DayOfWeek): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .with(TemporalAdjusters.nextOrSame(day))
        .toInstant().toEpochMilli()

private const val MILLIS_PER_YEAR = 365L * 24 * 60 * 60 * 1000
