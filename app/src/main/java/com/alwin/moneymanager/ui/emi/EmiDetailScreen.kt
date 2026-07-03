package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.ui.common.ConfirmDeleteDialog
import com.alwin.moneymanager.util.addMonths
import com.alwin.moneymanager.util.formatCurrency
import com.alwin.moneymanager.util.shortMonthYearLabel
import java.text.DateFormat
import java.util.Date

@Composable
fun EmiDetailScreen(
    emiId: Long,
    onBack: () -> Unit,
    viewModel: EmiDetailViewModel = hiltViewModel(),
) {
    val item by viewModel.emiWithProgress.collectAsState()
    val haptics = LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCongrats by remember { mutableStateOf(false) }
    var showUndoReopenWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.emi?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }, enabled = item != null) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit EMI")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete EMI")
                    }
                },
            )
        }
    ) { innerPadding ->
        val current = item
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
            // Precompute every cell's label + paid flag once, up front — chunked into rows of 6.
            // The date parsing/formatting is the expensive part; doing it here (keyed on the data)
            // instead of inside each cell keeps it off the scroll path, so scrolling stays smooth
            // even on long loans. The whole screen is then one LazyColumn; the action bar is pinned.
            val monthRows = remember(current.emi.startDateMillis, current.emi.totalMonths, current.payments) {
                val paid = current.payments.map { it.monthNumber }.toSet()
                (1..current.emi.totalMonths).map { month ->
                    MonthCellData(
                        monthNumber = month,
                        label = shortMonthYearLabel(addMonths(current.emi.startDateMillis, month - 1)),
                        isPaid = paid.contains(month),
                    )
                }.chunked(6)
            }
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Extra bottom room so the last months can scroll clear of the translucent bar.
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "overview", contentType = "overview") {
                        EmiOverviewCard(item = current, dateFormat = dateFormat)
                    }
                    item(key = "history_header", contentType = "header") {
                        Text(
                            "Payment history",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(
                        monthRows,
                        key = { it.first().monthNumber },
                        contentType = { "month_row" },
                    ) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { cell ->
                                MonthCell(cell = cell, modifier = Modifier.weight(1f))
                            }
                            // Keep cells the same size on a partial final row.
                            repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // Translucent action bar overlaying the list: content fades into it via the top
                // gradient (transparent → surface) so it reads as a floating layer, while the
                // buttons stay fully legible. Fully-transparent would let cells clash with the
                // button text — the fade keeps contrast without hiding the content underneath.
                val surface = MaterialTheme.colorScheme.surface
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to surface.copy(alpha = 0.9f),
                                1f to surface.copy(alpha = 0.9f),
                            )
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (current.emi.isCompleted) {
                                showUndoReopenWarning = true
                            } else {
                                viewModel.undoLastPayment()
                            }
                        },
                        enabled = current.paidMonths > 0,
                        modifier = Modifier.weight(1f),
                    ) { Text("Undo last payment") }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.markNextMonthPaid(onCompleted = { showCongrats = true })
                        },
                        enabled = current.paidMonths < current.emi.totalMonths,
                        modifier = Modifier.weight(1f),
                    ) { Text("Mark next month paid") }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete EMI?",
            message = "This will permanently delete this EMI and all its payment history.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteEmi(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    val current = item
    if (showEditDialog && current != null) {
        EmiFormDialog(
            title = "Edit EMI",
            initialName = current.emi.name,
            initialMonthlyAmount = current.emi.monthlyAmount.toString(),
            initialStartDateMillis = current.emi.startDateMillis,
            initialEndDateMillis = current.emi.endDateMillis,
            initialNotes = current.emi.notes,
            initialNotificationEnabled = current.emi.notificationEnabled,
            initialReminderDaysBefore = current.emi.reminderDaysBefore,
            initialLoanAmount = if (current.emi.loanAmount > 0) current.emi.loanAmount.toString() else "",
            minTotalMonths = maxOf(1, current.paidMonths),
            onDismiss = { showEditDialog = false },
            onConfirm = { form ->
                viewModel.updateEmi(form)
                showEditDialog = false
            },
        )
    }

    if (showUndoReopenWarning) {
        AlertDialog(
            onDismissRequest = { showUndoReopenWarning = false },
            title = { Text("Reopen this loan?") },
            text = {
                Text(
                    "${current?.emi?.name ?: "This EMI"} is marked as fully paid. Undoing the " +
                        "last payment will mark it as still in progress again."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUndoReopenWarning = false
                    viewModel.undoLastPayment()
                }) { Text("Undo") }
            },
            dismissButton = {
                TextButton(onClick = { showUndoReopenWarning = false }) { Text("Cancel") }
            },
        )
    }

    if (showCongrats) {
        PayoffCelebrationDialog(
            emiName = current?.emi?.name ?: "This EMI",
            onDismiss = { showCongrats = false },
        )
    }
}

@Composable
private fun EmiOverviewCard(item: EmiWithProgress, dateFormat: DateFormat) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OverviewStat(label = "Monthly amount", value = formatCurrency(item.emi.monthlyAmount))
                    Spacer(Modifier.height(14.dp))
                    OverviewStat(label = "Remaining", value = formatCurrency(item.remainingAmount))
                }
                EmiProgressRing(
                    paidMonths = item.paidMonths,
                    totalMonths = item.emi.totalMonths,
                    progress = item.progressPercent,
                    size = 84.dp,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            DetailRow(
                label = "Duration",
                value = "${dateFormat.format(Date(item.emi.startDateMillis))} – " +
                    dateFormat.format(Date(item.emi.endDateMillis)),
            )

            if (item.emi.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Column {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(item.emi.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item.totalInterest?.let { interest ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                DetailRow(label = "Loan amount", value = formatCurrency(item.emi.loanAmount))
                Spacer(Modifier.height(10.dp))
                DetailRow(label = "Total payable", value = formatCurrency(item.totalPayable))
                Spacer(Modifier.height(10.dp))
                DetailRow(
                    label = "Total interest",
                    value = formatCurrency(interest),
                    valueColor = MaterialTheme.colorScheme.error,
                    emphasize = true,
                )
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasize: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

private data class MonthCellData(
    val monthNumber: Int,
    val label: String,
    val isPaid: Boolean,
)

@Composable
private fun MonthCell(cell: MonthCellData, modifier: Modifier = Modifier) {
    val backgroundColor = if (cell.isPaid) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (cell.isPaid) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(8.dp)),
    ) {
        Text(
            text = cell.monthNumber.toString(),
            color = contentColor.copy(alpha = 0.7f),
            fontSize = 9.sp,
            lineHeight = 9.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
        )
        Text(
            text = cell.label,
            color = contentColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
