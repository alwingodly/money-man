package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.util.formatCurrency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChittiDetailScreen(
    chittiId: Long,
    onBack: () -> Unit,
    viewModel: ChittiDetailViewModel = hiltViewModel(),
) {
    val chittiWithProgress by viewModel.chittiWithProgress.collectAsState()
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showMarkReceivedDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<com.alwin.moneymanager.data.local.entity.ChittiPayment?>(null) }
    var deletingPayment by remember { mutableStateOf<com.alwin.moneymanager.data.local.entity.ChittiPayment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chittiWithProgress?.chitti?.name ?: "Chitti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        enabled = chittiWithProgress != null
                    ) {
                        Icon(Icons.Filled.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            if (chittiWithProgress != null && chittiWithProgress!!.remainingMonths > 0) {
                FloatingActionButton(onClick = { showAddPaymentDialog = true }) {
                    Icon(Icons.Filled.Add, "Add Payment")
                }
            }
        },
    ) { padding ->
        chittiWithProgress?.let { item ->
            val sortedPayments = remember(item.payments) {
                item.payments.sortedByDescending { it.monthNumber }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Summary Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Progress and frequency
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Progress: ${item.paidMonths} / ${item.chitti.totalMonths}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                item.chitti.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Amounts in a clean row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Total Paid",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                LcdAmountText(
                                    formatCurrency(item.totalPaid),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Received",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                LcdAmountText(
                                    formatCurrency(item.totalReceived),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Net position
                        Text(
                            "Net: ${formatCurrency(item.netPosition)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (item.netPosition >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        
                        // Mark as received button
                        if (item.chitti.receivedAmount == null && item.remainingMonths > 0) {
                            TextButton(
                                onClick = { showMarkReceivedDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark as Received")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Payment History
                item {
                    Text(
                        "Payment History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (item.payments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No payments yet.\nTap + to add first payment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(sortedPayments, key = { it.id }) { payment ->
                        PaymentRow(
                            payment = payment,
                            onEdit = { editingPayment = payment },
                            onDelete = { deletingPayment = payment }
                        )
                    }
                }
            }
        }
    }

    if (showAddPaymentDialog && chittiWithProgress != null) {
        AddPaymentDialog(
            chittiWithProgress = chittiWithProgress!!,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.addPayment(amount, note)
                showAddPaymentDialog = false
            },
        )
    }

    if (showMarkReceivedDialog && chittiWithProgress != null) {
        MarkReceivedDialog(
            onDismiss = { showMarkReceivedDialog = false },
            onConfirm = { amount ->
                viewModel.markReceived(amount)
                showMarkReceivedDialog = false
            },
        )
    }

    if (showEditDialog && chittiWithProgress != null) {
        EditChittiDialog(
            chitti = chittiWithProgress!!.chitti,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, notes, frequency ->
                viewModel.updateChitti(name, notes, frequency)
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Chitti?") },
            text = { Text("This will permanently delete this chitti and all its payment records. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChitti()
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (editingPayment != null) {
        EditPaymentDialog(
            payment = editingPayment!!,
            onDismiss = { editingPayment = null },
            onConfirm = { amount, note ->
                viewModel.updatePayment(editingPayment!!.copy(amount = amount, note = note))
                editingPayment = null
            }
        )
    }

    if (deletingPayment != null) {
        AlertDialog(
            onDismissRequest = { deletingPayment = null },
            title = { Text("Delete Payment?") },
            text = { Text("Delete payment for Month ${deletingPayment!!.monthNumber}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePayment(deletingPayment!!)
                        deletingPayment = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PaymentRow(
    payment: com.alwin.moneymanager.data.local.entity.ChittiPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Month ${payment.monthNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val date = LocalDate.ofInstant(
                    Instant.ofEpochMilli(payment.paidDateMillis),
                    ZoneId.systemDefault()
                )
                Text(
                    date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                formatCurrency(payment.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPaymentDialog(
    chittiWithProgress: com.alwin.moneymanager.data.repository.ChittiWithProgress,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val nextMonth = chittiWithProgress.paidMonths + 1
    val isValid = amount.toDoubleOrNull() != null && amount.toDouble() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment - Month $nextMonth") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g., 2500") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g., After dividend") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(amount.toDouble(), note.trim()) },
                enabled = isValid
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun MarkReceivedDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    val isValid = amount.toDoubleOrNull() != null && amount.toDouble() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Chitti Received") },
        text = {
            Column {
                Text(
                    "Enter the amount you received from this chitti:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount Received") },
                    placeholder = { Text("e.g., 50000") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(amount.toDouble()) },
                enabled = isValid
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EditPaymentDialog(
    payment: com.alwin.moneymanager.data.local.entity.ChittiPayment,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
) {
    var amount by remember { mutableStateOf(payment.amount.toString()) }
    var note by remember { mutableStateOf(payment.note) }
    val isValid = amount.toDoubleOrNull() != null && amount.toDouble() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Payment - Month ${payment.monthNumber}") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(amount.toDouble(), note.trim()) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EditChittiDialog(
    chitti: com.alwin.moneymanager.data.local.entity.Chitti,
    onDismiss: () -> Unit,
    onConfirm: (String, String, com.alwin.moneymanager.data.local.entity.ChittiFrequency) -> Unit,
) {
    var name by remember { mutableStateOf(chitti.name) }
    var notes by remember { mutableStateOf(chitti.notes) }
    var frequency by remember { mutableStateOf(chitti.frequency) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Chitti") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Chitti Name") },
                    modifier = Modifier.fillMaxWidth(),
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
                        com.alwin.moneymanager.data.local.entity.ChittiFrequency.entries.forEach { freq ->
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
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Note: You cannot change the duration (total months) after creation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(name.trim(), notes.trim(), frequency) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
