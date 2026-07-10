package com.alwin.moneymanager.ui.emi

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.util.formatCurrency

@Composable
fun EmiListScreen(
    onEmiClick: (Long) -> Unit,
    onClosedLoansClick: () -> Unit,
    viewModel: EmiViewModel = hiltViewModel(),
) {
    val emiList by viewModel.emiList.collectAsState()
    val monthSummary by viewModel.monthSummary.collectAsState()
    val periodTotals by viewModel.periodTotals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showTotals by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EMIs")
                        if (emiList.isNotEmpty()) {
                            Text(
                                "This month: ${formatCurrency(monthSummary.paidAmount)} paid · " +
                                    "${formatCurrency(monthSummary.dueAmount)} due",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Monthly & yearly totals") },
                                onClick = {
                                    menuExpanded = false
                                    showTotals = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Closed loans") },
                                onClick = {
                                    menuExpanded = false
                                    onClosedLoansClick()
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add EMI")
            }
        },
    ) { innerPadding ->
        if (emiList.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CreditCard,
                title = "No loans yet",
                subtitle = "Tap + below to add a loan or EMI — we'll track your payments and due dates for you.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, innerPadding.calculateTopPadding(), 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(emiList, key = { it.emi.id }) { item ->
                    EmiCard(item = item, onClick = { onEmiClick(item.emi.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        EmiFormDialog(
            title = "Add EMI",
            onDismiss = { showAddDialog = false },
            onConfirm = { form ->
                viewModel.addEmi(form)
                showAddDialog = false
            },
        )
    }

    if (showTotals) {
        EmiTotalsDialog(totals = periodTotals, onDismiss = { showTotals = false })
    }
}

@Composable
internal fun EmiCard(item: EmiWithProgress, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.emi.name, fontWeight = FontWeight.Bold)
                if (item.emi.isCompleted) {
                    Text("Completed")
                }
                Text("Remaining: ${formatCurrency(item.remainingAmount)}")
            }
            EmiProgressRing(
                paidMonths = item.paidMonths,
                totalMonths = item.emi.totalMonths,
                progress = item.progressPercent,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
