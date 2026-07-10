package com.alwin.moneymanager.ui.expense

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.util.formatCurrency

@Composable
fun ExpenseSummaryScreen(
    onBack: () -> Unit,
    viewModel: ExpenseSummaryViewModel = hiltViewModel(),
) {
    val mode by viewModel.mode.collectAsState()
    val summaries by viewModel.summaries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val expandedKeys = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = mode == SummaryMode.MONTH,
                    onClick = { viewModel.setMode(SummaryMode.MONTH) },
                    label = { Text("By month") },
                )
                FilterChip(
                    selected = mode == SummaryMode.YEAR,
                    onClick = { viewModel.setMode(SummaryMode.YEAR) },
                    label = { Text("By year") },
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                summaries.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.ExpandMore,
                        title = "Nothing to summarise yet",
                        subtitle = "Add some expenses and your monthly and yearly totals will appear here.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(summaries, key = { it.sortKey }) { period ->
                            PeriodCard(
                                period = period,
                                expanded = expandedKeys[period.sortKey] == true,
                                onToggle = { expandedKeys[period.sortKey] = expandedKeys[period.sortKey] != true },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(
    period: PeriodSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(period.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${period.count} ${if (period.count == 1) "expense" else "expenses"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LcdAmountText(
                    formatCurrency(period.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(start = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                period.categoryBreakdown.forEach { cat ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium)
                        LcdAmountText(
                            formatCurrency(cat.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
