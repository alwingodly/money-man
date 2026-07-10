package com.alwin.moneymanager.ui.saving

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.local.entity.SavingContribution
import com.alwin.moneymanager.data.repository.SavingWithProgress
import com.alwin.moneymanager.ui.common.ConfirmDeleteDialog
import com.alwin.moneymanager.util.formatCurrency
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun SavingDetailScreen(
    onBack: () -> Unit,
    viewModel: SavingDetailViewModel = hiltViewModel(),
) {
    val item by viewModel.savingWithProgress.collectAsState()
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddContribution by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(item?.saving?.name ?: "") },
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
                    item(key = "overview") { SavingOverviewCard(item = current) }
                    item(key = "history_header") {
                        Text(
                            "Contributions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    if (current.contributions.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                "No contributions yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(current.contributions.sortedByDescending { it.dateMillis }, key = { it.id }) { c ->
                            ContributionRow(
                                contribution = c,
                                dateFormat = dateFormat,
                                onDelete = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.deleteContribution(c)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Contribution removed",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) viewModel.restoreContribution(c)
                                    }
                                },
                            )
                        }
                    }
                }

                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddContribution = true
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text("Add to savings") }
                }
            }
        }
    }

    val current = item
    if (showAddContribution && current != null) {
        AddContributionDialog(
            onDismiss = { showAddContribution = false },
            onConfirm = { amount, dateMillis ->
                viewModel.addContribution(amount, dateMillis)
                showAddContribution = false
            },
        )
    }

    if (showEditDialog && current != null) {
        SavingFormDialog(
            title = "Edit saving",
            initialName = current.saving.name,
            initialTarget = current.saving.targetAmount?.let { trimAmount(it) } ?: "",
            initialNote = current.saving.note,
            onDismiss = { showEditDialog = false },
            onConfirm = { form ->
                viewModel.updateSaving(form)
                showEditDialog = false
            },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete saving?",
            message = "This will permanently delete this savings pot and its whole history.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteSaving(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun SavingOverviewCard(item: SavingWithProgress) {
    val saving = item.saving
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Saved so far",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatCurrency(item.totalSaved),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            item.progress?.let { progress ->
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                val goalText = if (item.isAchieved) {
                    "Goal of ${formatCurrency(saving.targetAmount!!)} reached! 🎉"
                } else {
                    "${formatCurrency(item.remaining ?: 0.0)} left of ${formatCurrency(saving.targetAmount!!)}"
                }
                Text(goalText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (saving.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("Note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(saving.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ContributionRow(contribution: SavingContribution, dateFormat: DateFormat, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("+${formatCurrency(contribution.amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                dateFormat.format(Date(contribution.dateMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove contribution", tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** "500.0" -> "500" so the edit dialog's target field reads cleanly. */
private fun trimAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
