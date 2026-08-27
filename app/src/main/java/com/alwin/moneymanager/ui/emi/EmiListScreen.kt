package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.util.formatCurrency

@Composable
fun EmiListScreen(
    onEmiClick: (Long) -> Unit,
    onChittiClick: (Long) -> Unit = {},
    onClosedLoansClick: () -> Unit,
    viewModel: EmiViewModel = hiltViewModel(),
) {
    val emiList by viewModel.emiList.collectAsState()
    val chittiList by viewModel.chittiList.collectAsState()
    val monthSummary by viewModel.monthSummary.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddEmiDialog by remember { mutableStateOf(false) }
    var showAddChittiDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showTotals by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EMI & Chitti")
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
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        showAddEmiDialog = true
                    } else {
                        showAddChittiDialog = true
                    }
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("EMI") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Chitti") }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> {
                    // EMI Tab Content
                    if (emiList.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.CreditCard,
                            title = "No loans yet",
                            subtitle = "Tap + below to add a loan or EMI — we'll track your payments and due dates for you.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val totalRemaining = emiList.filter { !it.emi.isCompleted }.sumOf { it.remainingAmount }
                        val totalPaid = emiList.sumOf { (it.emi.monthlyAmount * it.paidMonths).toLong() }.toDouble()
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "To Pay: ${formatCurrency(totalRemaining)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Paid: ${formatCurrency(totalPaid)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            items(emiList, key = { "emi_${it.emi.id}" }) { item ->
                                EmiCard(item = item, onClick = { onEmiClick(item.emi.id) })
                            }
                        }
                    }
                }
                1 -> {
                    // Chitti Tab Content
                    if (chittiList.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.CreditCard,
                            title = "No chittis yet",
                            subtitle = "Tap + below to add a chitti — we'll track your payments and received amounts for you.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val totalPaid = chittiList.sumOf { it.totalPaid }
                        val totalReceived = chittiList.sumOf { it.totalReceived }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Total Paid: ${formatCurrency(totalPaid)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Received: ${formatCurrency(totalReceived)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            items(chittiList, key = { "chitti_${it.chitti.id}" }) { item ->
                                ChittiCard(item = item, onClick = { onChittiClick(item.chitti.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEmiDialog) {
        EmiFormDialog(
            title = "Add EMI",
            onDismiss = { showAddEmiDialog = false },
            onConfirm = { form ->
                viewModel.addEmi(form)
                showAddEmiDialog = false
            },
        )
    }

    if (showAddChittiDialog) {
        AddChittiDialog(
            onDismiss = { showAddChittiDialog = false },
            onConfirm = { form ->
                viewModel.addChitti(form)
                showAddChittiDialog = false
            },
        )
    }

    if (showTotals) {
        val periodTotals = remember { viewModel.periodTotalsSnapshot() }
        EmiTotalsDialog(totals = periodTotals, onDismiss = { showTotals = false })
    }

}

@Composable
internal fun EmiCard(item: EmiWithProgress, onClick: () -> Unit) {
    // macOS-style flat card
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.emi.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (item.emi.isCompleted) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Remaining: ${formatCurrency(item.remainingAmount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            EmiProgressRing(
                paidMonths = item.paidMonths,
                totalMonths = item.emi.totalMonths,
                progress = item.progressPercent,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
internal fun ChittiCard(item: com.alwin.moneymanager.data.repository.ChittiWithProgress, onClick: () -> Unit) {
    // macOS-style flat card (matching EMI)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.chitti.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (item.chitti.isCompleted) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Net: ${formatCurrency(item.netPosition)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    item.chitti.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            EmiProgressRing(
                paidMonths = item.paidMonths,
                totalMonths = item.chitti.totalMonths,
                progress = item.progressPercent,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
