package com.alwin.moneymanager.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val _categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    val categories: StateFlow<List<ExpenseCategory>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _periodFilter = MutableStateFlow(PeriodFilter(PeriodGranularity.ALL))
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter.asStateFlow()

    private val categoryExpenses = _selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) flowOf(emptyList()) else repository.getExpensesByCategory(categoryId)
    }

    val filteredExpenses: StateFlow<List<Expense>> =
        combine(categoryExpenses, _periodFilter) { expenses, filter ->
            filterByPeriod(expenses, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getAllCategories().collect { list ->
                _categories.value = list
                if (_selectedCategoryId.value == null && list.isNotEmpty()) {
                    _selectedCategoryId.value = list.first().id
                }
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val id = repository.addCategory(name)
            _selectedCategoryId.value = id
        }
    }

    fun setGranularity(granularity: PeriodGranularity) {
        _periodFilter.value = _periodFilter.value.copy(granularity = granularity)
    }

    fun shiftPeriodBy(delta: Int) {
        val current = _periodFilter.value
        _periodFilter.value = current.copy(referenceMillis = shiftPeriod(current, delta))
    }

    fun addExpense(amount: Double, note: String, dateMillis: Long, isCreditCard: Boolean) {
        val categoryId = _selectedCategoryId.value ?: return
        viewModelScope.launch { repository.addExpense(categoryId, amount, note, dateMillis, isCreditCard) }
    }

    fun updateExpense(expense: Expense, amount: Double, note: String, dateMillis: Long, isCreditCard: Boolean) {
        viewModelScope.launch { repository.updateExpense(expense, amount, note, dateMillis, isCreditCard) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }
}
