package com.alwin.moneymanager.ui.debt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.DebtWithProgress
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.util.formatCurrency
import com.alwin.moneymanager.util.shortMonthYearLabel

@Composable
fun DebtListScreen(
    onDebtClick: (Long) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: DebtViewModel = hiltViewModel(),
) {
    val debts by viewModel.debts.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val query by viewModel.query.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val existingNames by viewModel.existingNames.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debts") },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Filled.History, contentDescription = "Settled debts history")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add debt")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DebtSummaryHeader(toCollect = summary.totalToCollect, toPay = summary.totalToPay)

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search by name or note") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                debts.isEmpty() && query.isBlank() -> {
                    EmptyState(
                        icon = Icons.Filled.Add,
                        title = "No debts yet",
                        subtitle = "Tap + to note money you'll get back or need to give — like a debt book that never gets lost.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                debts.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No matches",
                        subtitle = "Nothing found for \"$query\".",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    // One flat list: every person once, most recent first. Direction is carried by
                    // the signed, colour-coded amount on each row rather than by section headers.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(debts, key = { it.debt.id }) { item ->
                            DebtRow(item = item, onClick = { onDebtClick(item.debt.id) })
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        DebtFormDialog(
            title = "New debt",
            existingNames = existingNames,
            onDismiss = { showAddDialog = false },
            onConfirm = { form ->
                viewModel.addDebt(form)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun DebtSummaryHeader(toCollect: Double, toPay: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryTile(
            label = "You'll get",
            value = toCollect,
            container = getContainerColor(),
            onContainer = getColor(),
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            label = "You'll give",
            value = toPay,
            container = MaterialTheme.colorScheme.errorContainer,
            onContainer = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Double,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = onContainer)
            Text(
                formatCurrency(value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = onContainer,
            )
        }
    }
}

@Composable
internal fun DebtRow(item: DebtWithProgress, onClick: () -> Unit) {
    val debt = item.debt
    val owesYou = item.netBalance > 0
    val accent = if (owesYou) getColor() else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(debt.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                debt.dueDateMillis?.let { due ->
                    Text(
                        "Due ${shortMonthYearLabel(due).replace("\n", " ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Signed + colour-coded: green "+" = they owe you, red "−" = you owe them.
            Text(
                (if (owesYou) "+" else "−") + formatCurrency(item.outstanding),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
