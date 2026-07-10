package com.alwin.moneymanager.ui.expense

import android.app.Activity
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.data.repository.PremiumLimits
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.ui.common.PaywallDialog
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.util.formatCurrency
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

private val granularities = listOf(
    PeriodGranularity.ALL to "All",
    PeriodGranularity.DAY to "Day",
    PeriodGranularity.MONTH to "Month",
    PeriodGranularity.YEAR to "Year",
)

@Composable
fun ExpenseScreen(
    onDateSelected: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onSummaryClick: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel(),
) {
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showCategoryPaywall by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val activity = LocalContext.current as Activity
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val periodFilter by viewModel.periodFilter.collectAsState()
    val expenses by viewModel.filteredExpenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMonthSpendForSelected by viewModel.currentMonthSpendForSelectedCategory.collectAsState()
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    val total = expenses.sumOf { it.amount }
    var menuExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val onDeleteExpense: (Expense) -> Unit = { expense ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.deleteExpense(expense)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Expense deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreExpense(expense)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                actions = {
                    IconButton(onClick = onSummaryClick) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Monthly and yearly summary")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search expenses")
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "View total for a specific day")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Change time range")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            granularities.forEach { (granularity, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setGranularity(granularity)
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                // Always actionable: with no categories yet, "+" starts by creating one; otherwise
                // it opens the add-expense dialog (which now has its own category picker).
                onClick = {
                    if (categories.isEmpty()) showAddCategoryDialog = true else showAddExpenseDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FilterPanel(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
                onAddCategoryClick = {
                    if (!isPremium && categories.size >= PremiumLimits.FREE_CATEGORY_LIMIT) {
                        showCategoryPaywall = true
                    } else {
                        showAddCategoryDialog = true
                    }
                },
                onEditCategoryClick = { editingCategory = it },
            )

            TotalSummaryCard(
                total = total,
                periodFilter = periodFilter,
                onShiftPeriod = viewModel::shiftPeriodBy,
                selectedCategory = selectedCategory,
                spentThisMonth = currentMonthSpendForSelected,
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                categories.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Receipt,
                        title = "No categories yet",
                        subtitle = "Add a category (like Food or Travel) to start tracking your spending.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                expenses.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Receipt,
                        title = "Nothing here yet",
                        subtitle = "Expenses you add for this period will show up here.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                periodFilter.granularity == PeriodGranularity.DAY -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(expenses, key = { it.id }) { expense ->
                            ExpenseRow(
                                expense = expense,
                                showDate = false,
                                onDelete = { onDeleteExpense(expense) },
                                onEditClick = { editingExpense = expense },
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        groupExpensesByDay(expenses).forEach { (date, dayExpenses) ->
                            item(key = "header_$date") {
                                DayHeader(
                                    date = date,
                                    subtotal = dayExpenses.sumOf { it.amount },
                                )
                            }
                            items(dayExpenses, key = { it.id }) { expense ->
                                ExpenseRow(
                                    expense = expense,
                                    showDate = false,
                                    onDelete = { onDeleteExpense(expense) },
                                    onEditClick = { editingExpense = expense },
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog && categories.isNotEmpty()) {
        AddExpenseDialog(
            categories = categories,
            initialCategoryId = selectedCategoryId,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { categoryId, amount, note, dateMillis, isCreditCard ->
                viewModel.addExpense(categoryId, amount, note, dateMillis, isCreditCard)
                showAddExpenseDialog = false
            },
        )
    }

    editingExpense?.let { expense ->
        AddExpenseDialog(
            categories = categories,
            existing = expense,
            onDismiss = { editingExpense = null },
            onConfirm = { categoryId, amount, note, dateMillis, isCreditCard ->
                viewModel.updateExpense(expense, categoryId, amount, note, dateMillis, isCreditCard)
                editingExpense = null
            },
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, budgetLimit ->
                viewModel.addCategory(name, budgetLimit)
                showAddCategoryDialog = false
            },
        )
    }

    if (showCategoryPaywall) {
        PaywallDialog(
            message = "Whoa, category #${categories.size + 1}! Grab me a ₹9 coffee and add " +
                "as many categories as you like, forever.",
            onDismiss = { showCategoryPaywall = false },
            onUnlock = {
                viewModel.purchasePremium(activity)
                showCategoryPaywall = false
            },
        )
    }

    editingCategory?.let { category ->
        AddCategoryDialog(
            existing = category,
            onDismiss = { editingCategory = null },
            onConfirm = { name, budgetLimit ->
                viewModel.updateCategory(category, name, budgetLimit)
                editingCategory = null
            },
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onDateSelected)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FilterPanel(
    categories: List<ExpenseCategory>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long) -> Unit,
    onAddCategoryClick: () -> Unit,
    onEditCategoryClick: (ExpenseCategory) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = category.id == selectedCategoryId
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category.id) },
                label = { Text(category.name) },
                trailingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit ${category.name}",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp).clickable { onEditCategoryClick(category) },
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = onAddCategoryClick,
                label = { Icon(Icons.Filled.Add, contentDescription = "Add category", modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
private fun TotalSummaryCard(
    total: Double,
    periodFilter: PeriodFilter,
    onShiftPeriod: (Int) -> Unit,
    selectedCategory: ExpenseCategory?,
    spentThisMonth: Double,
) {
    val showNav = periodFilter.granularity != PeriodGranularity.ALL
    val budget = selectedCategory?.budgetLimit
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = if (showNav) 4.dp else 16.dp,
                end = if (showNav) 4.dp else 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showNav) {
                IconButton(onClick = { onShiftPeriod(-1) }) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous period",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = if (showNav) Alignment.CenterHorizontally else Alignment.Start) {
                Text(
                    if (showNav) periodLabel(periodFilter) else "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                LcdAmountText(
                    formatCurrency(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (budget != null) {
                val remaining = budget - spentThisMonth
                val isOver = remaining < 0
                val amountColor = when {
                    isOver -> MaterialTheme.colorScheme.error
                    spentThisMonth >= budget * 0.8 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        formatCurrency(kotlin.math.abs(remaining)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                    Text(
                        if (isOver) "over" else "left",
                        style = MaterialTheme.typography.labelSmall,
                        color = amountColor,
                    )
                }
            }
            if (showNav) {
                IconButton(onClick = { onShiftPeriod(1) }) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next period",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, subtotal: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            dayHeaderLabel(date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LcdAmountText(
            formatCurrency(subtotal),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    showDate: Boolean,
    onDelete: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onEditClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LcdAmountText(formatCurrency(expense.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (expense.isCreditCard) {
                        Icon(
                            Icons.Filled.CreditCard,
                            contentDescription = "Paid by credit card",
                            modifier = Modifier.padding(start = 6.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (expense.note.isNotBlank()) {
                    Text(
                        expense.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showDate) {
                    Text(
                        dateFormat.format(Date(expense.dateMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete expense")
            }
        }
    }
}
