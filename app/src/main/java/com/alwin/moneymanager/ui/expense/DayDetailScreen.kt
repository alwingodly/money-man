package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.ui.common.AppListCard
import com.alwin.moneymanager.ui.common.ConfirmDeleteDialog
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.util.formatCurrency
import kotlinx.coroutines.launch

@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel(),
) {
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryNameById = remember(categories) { categories.associate { it.id to it.name } }
    val total = expenses.sumOf { it.amount }
    val extraTotal = expenses.filter { it.isExtra }.sumOf { it.amount }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expensePendingDelete by remember { mutableStateOf<Expense?>(null) }

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
                title = { Text(dayHeaderLabel(viewModel.date)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // macOS-style flat total card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    LcdAmountText(
                        formatCurrency(total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (extraTotal > 0) {
                        Text(
                            "Extra included ${formatCurrency(extraTotal)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (expenses.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Receipt,
                    title = "No expenses on this day",
                    subtitle = "Add one from the Expenses tab.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        DayExpenseRow(
                            expense = expense,
                            categoryName = categoryNameById[expense.categoryId] ?: "Unknown",
                            onEditClick = { editingExpense = expense },
                        )
                    }
                }
            }
        }
    }

    editingExpense?.let { expense ->
        AddExpenseDialog(
            categories = categories,
            existing = expense,
            onDismiss = { editingExpense = null },
            onConfirm = { categoryId, amount, note, dateMillis, isCreditCard, isExtra ->
                viewModel.updateExpense(expense, categoryId, amount, note, dateMillis, isCreditCard, isExtra)
                editingExpense = null
            },
            onDelete = { expensePendingDelete = expense },
        )
    }

    expensePendingDelete?.let { expense ->
        ConfirmDeleteDialog(
            title = "Delete expense?",
            message = "This will permanently delete this expense.",
            onConfirm = {
                expensePendingDelete = null
                editingExpense = null
                onDeleteExpense(expense)
            },
            onDismiss = { expensePendingDelete = null },
        )
    }

}

@Composable
private fun DayExpenseRow(
    expense: Expense,
    categoryName: String,
    onEditClick: () -> Unit,
) {
    AppListCard(onClick = onEditClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LcdAmountText(
                    formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (expense.isExtra) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.extraAmountChip(expense.isExtra),
                )
                if (expense.isCreditCard) {
                    Icon(
                        Icons.Filled.CreditCard,
                        contentDescription = "Paid by credit card",
                        modifier = Modifier.padding(start = 6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(categoryColor(expense.categoryId), CircleShape),
                )
                Text(
                    categoryName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (expense.note.isNotBlank()) {
                Text(
                    expense.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
