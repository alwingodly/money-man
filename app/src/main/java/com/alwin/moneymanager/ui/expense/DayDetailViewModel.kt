package com.alwin.moneymanager.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.data.repository.ExpenseRepository
import com.alwin.moneymanager.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val referenceMillis: Long = checkNotNull(savedStateHandle[Destination.ExpenseDayDetail.ARG_DATE_MILLIS])
    val date: LocalDate = Instant.ofEpochMilli(referenceMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    val expenses: StateFlow<List<Expense>> = run {
        val (start, end) = dayRange(date)
        repository.getExpensesForPeriod(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val categories: StateFlow<List<ExpenseCategory>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateExpense(expense: Expense, amount: Double, note: String, dateMillis: Long, isCreditCard: Boolean) {
        viewModelScope.launch { repository.updateExpense(expense, amount, note, dateMillis, isCreditCard) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }
}
