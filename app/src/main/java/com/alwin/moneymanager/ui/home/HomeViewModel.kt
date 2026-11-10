package com.alwin.moneymanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.data.repository.ExpenseRepository
import com.alwin.moneymanager.data.repository.RangeSpend
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.data.repository.HomeRepository
import com.alwin.moneymanager.data.repository.HomeSummary
import com.alwin.moneymanager.data.repository.RecentExpenseItem
import com.alwin.moneymanager.data.repository.SpendRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RECENT_ACTIVITY_LIMIT = 5

private val EMPTY_SUMMARY = HomeSummary(
    todayTotal = 0.0,
    monthExpenseOnly = 0.0,
    monthIncludingEmi = 0.0,
    monthlyAverageExpenseOnly = 0.0,
    monthlyAverageIncludingEmi = 0.0,
    emiDueThisMonth = 0.0,
    totalEmiOutstanding = 0.0,
    creditCardThisMonth = 0.0,
    savingsThisMonth = 0.0,
    extraThisMonth = 0.0,
    lastMonthToDate = 0.0,
    debtToCollect = 0.0,
    debtToPay = 0.0,
)

data class HomeCategories(val ready: Boolean, val items: List<ExpenseCategory>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: HomeRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _spendRange = MutableStateFlow(SpendRange.MONTH)
    val spendRange: StateFlow<SpendRange> = _spendRange.asStateFlow()

    // The dashboard combines ~7 Room query flows; collecting it once and fanning both `summary` and
    // `isLoading` out of this single hot StateFlow avoids re-running that whole query set a second
    // time just to know whether the first result has landed. null = not loaded yet.
    private val summaryState: StateFlow<HomeSummary?> = repository.getHomeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val summary: StateFlow<HomeSummary> = summaryState
        .map { it ?: EMPTY_SUMMARY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EMPTY_SUMMARY)

    val activeEmis: StateFlow<List<EmiWithProgress>> = repository.getActiveEmis()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentActivity: StateFlow<List<RecentExpenseItem>> = repository.getRecentActivity(RECENT_ACTIVITY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySpend: StateFlow<RangeSpend> = _spendRange
        .flatMapLatest { repository.getCategorySpend(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RangeSpend(emptyList(), 0.0))

    fun setSpendRange(range: SpendRange) {
        _spendRange.value = range
    }

    val categories: StateFlow<HomeCategories> = expenseRepository.getAllCategories()
        .map { HomeCategories(ready = true, items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeCategories(ready = false, items = emptyList()))

    fun addExpense(
        categoryId: Long,
        amount: Double,
        note: String,
        dateMillis: Long,
        isCreditCard: Boolean,
        isExtra: Boolean,
    ) {
        viewModelScope.launch {
            expenseRepository.addExpense(categoryId, amount, note, dateMillis, isCreditCard, isExtra)
        }
    }

    fun addCategory(name: String, budgetLimit: Double?) {
        viewModelScope.launch { expenseRepository.addCategory(name, budgetLimit) }
    }

    // True until the dashboard's first data arrives, so Home shows a spinner instead of briefly
    // flashing the "Welcome" empty state (which looks identical to a genuinely empty account) to
    // a returning user while Room loads.
    val isLoading: StateFlow<Boolean> = summaryState
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
