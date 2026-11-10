package com.alwin.moneymanager.ui.expense

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.runtime.LaunchedEffect
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
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.util.formatCurrency

@Composable
fun ExpenseSummaryScreen(
    onBack: () -> Unit,
    startOnChart: Boolean = false,
    viewModel: ExpenseSummaryViewModel = hiltViewModel(),
) {
    val mode by viewModel.mode.collectAsState()
    val summaries by viewModel.summaries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var expandedKeys by remember { mutableStateOf(setOf<Long>()) }
    var openedDefault by remember(mode) { mutableStateOf(false) }
    var showChart by remember { mutableStateOf(startOnChart) }

    LaunchedEffect(mode, summaries) {
        if (!openedDefault && summaries.isNotEmpty()) {
            expandedKeys = setOf(summaries.first().sortKey)
            openedDefault = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (summaries.isNotEmpty()) {
                        IconButton(onClick = { showChart = !showChart }) {
                            Icon(
                                if (showChart) Icons.Filled.ViewList else Icons.Filled.BarChart,
                                contentDescription = if (showChart) "Show list" else "Show chart",
                            )
                        }
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
                        icon = Icons.Filled.BarChart,
                        title = "Nothing to summarise yet",
                        subtitle = "Add some expenses and your monthly and yearly totals will appear here.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    val chartPeriod = summaries.first()
                    if (showChart) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp),
                                    )
                                    .padding(12.dp),
                            ) {
                                Text(
                                    chartPeriod.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                LcdAmountText(
                                    formatCurrency(chartPeriod.total),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                ActualExtraLine(
                                    total = chartPeriod.total,
                                    extra = chartPeriod.extraTotal,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                CategorySpendChart(
                                    categories = chartPeriod.categoryBreakdown,
                                    detailed = true,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(summaries, key = { it.sortKey }) { period ->
                            PeriodCard(
                                period = period,
                                expanded = period.sortKey in expandedKeys,
                                onToggle = {
                                    expandedKeys = if (period.sortKey in expandedKeys) {
                                        expandedKeys - period.sortKey
                                    } else {
                                        expandedKeys + period.sortKey
                                    }
                                },
                            )
                        }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
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
                    contentDescription = if (expanded) "Close ${period.label}" else "Open ${period.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            ActualExtraLine(
                total = period.total,
                extra = period.extraTotal,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (expanded) {
            period.categoryBreakdown.filter { it.amount > 0.0 }.forEach { cat ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(categoryColor(cat.categoryId), CircleShape),
                        )
                        Text(
                            cat.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
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

@Composable
private fun ActualExtraLine(
    total: Double,
    extra: Double,
    modifier: Modifier = Modifier,
) {
    val actual = (total - extra).coerceAtLeast(0.0)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Actual ${formatCurrency(actual)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Extra ${formatCurrency(extra)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
