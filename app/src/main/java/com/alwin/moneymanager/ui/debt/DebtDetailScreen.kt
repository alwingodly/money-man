package com.alwin.moneymanager.ui.debt

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.local.entity.DebtEntry
import com.alwin.moneymanager.data.repository.DebtWithProgress
import com.alwin.moneymanager.ui.common.ConfirmDeleteDialog
import com.alwin.moneymanager.util.formatCurrency
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun DebtDetailScreen(
    onBack: () -> Unit,
    viewModel: DebtDetailViewModel = hiltViewModel(),
) {
    val item by viewModel.debtWithProgress.collectAsState()
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showGaveMore by remember { mutableStateOf(false) }
    var showGotPaid by remember { mutableStateOf(false) }
    var showSettleConfirm by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(item?.debt?.personName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }, enabled = item != null) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, enabled = item != null) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = item
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "overview") { DebtOverviewCard(item = current, dateFormat = dateFormat) }
                    item(key = "history_header") {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    if (current.entries.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                "No entries yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(current.entries.sortedByDescending { it.dateMillis }, key = { it.id }) { entry ->
                            EntryRow(
                                entry = entry,
                                dateFormat = dateFormat,
                                onDelete = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.deleteEntry(entry)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Entry removed",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(entry)
                                    }
                                },
                            )
                        }
                    }
                }

                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        // Shortcut: clear the whole balance in one tap instead of typing the exact
                        // outstanding amount. Only shown when there's actually something to settle.
                        if (!current.isSettled && current.outstanding > 0) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showSettleConfirm = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Settle up (${formatCurrency(current.outstanding)})") }
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showGotPaid = true
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("I owe them") }
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showGaveMore = true
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("They owe me") }
                        }
                    }
                }
            }
        }
    }

    val current = item
    if (showGaveMore && current != null) {
        DebtAmountDialog(
            title = "${current.debt.personName} owes me",
            onDismiss = { showGaveMore = false },
            onConfirm = { amount, dateMillis ->
                viewModel.addGiven(amount, dateMillis)
                showGaveMore = false
            },
        )
    }
    if (showGotPaid && current != null) {
        DebtAmountDialog(
            title = "I owe ${current.debt.personName}",
            onDismiss = { showGotPaid = false },
            onConfirm = { amount, dateMillis ->
                viewModel.addGot(amount, dateMillis)
                showGotPaid = false
            },
        )
    }

    if (showSettleConfirm && current != null && !current.isSettled) {
        val name = current.debt.personName
        val amount = formatCurrency(current.outstanding)
        AlertDialog(
            onDismissRequest = { showSettleConfirm = false },
            title = { Text("Settle up?") },
            text = {
                Text(
                    if (current.isOwedToMe == true) {
                        "Record that $name paid you $amount in full? This clears the balance."
                    } else {
                        "Record that you paid $name $amount in full? This clears the balance."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSettleConfirm = false
                    viewModel.settleUp { entry ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Settled up with $name",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.deleteEntry(entry)
                        }
                    }
                }) { Text("Settle") }
            },
            dismissButton = {
                TextButton(onClick = { showSettleConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showEditDialog && current != null) {
        DebtFormDialog(
            title = "Edit debt",
            initialPersonName = current.debt.personName,
            initialNote = current.debt.note,
            initialDueDateMillis = current.debt.dueDateMillis,
            initialNotificationEnabled = current.debt.notificationEnabled,
            showAmount = false,
            onDismiss = { showEditDialog = false },
            onConfirm = { form ->
                viewModel.updateDebt(form)
                showEditDialog = false
            },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete debt?",
            message = "This will permanently delete this person's debt and its whole history.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteDebt(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun DebtOverviewCard(item: DebtWithProgress, dateFormat: DateFormat) {
    val debt = item.debt
    val owesYou = item.netBalance > 0
    val accent = if (owesYou) getColor() else MaterialTheme.colorScheme.error
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                when {
                    item.isSettled -> "All settled with ${debt.personName}"
                    owesYou -> "${debt.personName} owes you"
                    else -> "You owe ${debt.personName}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatCurrency(item.outstanding),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "You gave ${formatCurrency(item.totalGiven)} · You got ${formatCurrency(item.totalGot)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            debt.dueDateMillis?.let { due ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Due date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateFormat.format(Date(due)), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (debt.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("Note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(debt.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: DebtEntry,
    dateFormat: DateFormat,
    onDelete: () -> Unit,
) {
    // "You gave" pushes the balance toward them owing you (+); "You got" the other way (−).
    val label = if (entry.isGiven) "You gave" else "You got"
    val sign = if (entry.isGiven) "+" else "−"
    val amountColor: Color = if (entry.isGiven) getColor() else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            if (entry.isGiven) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = amountColor,
            modifier = Modifier.padding(end = 10.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                dateFormat.format(Date(entry.dateMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "$sign${formatCurrency(entry.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove entry", tint = MaterialTheme.colorScheme.error)
        }
    }
}
