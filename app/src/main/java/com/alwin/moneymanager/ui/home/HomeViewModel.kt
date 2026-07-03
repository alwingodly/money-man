package com.alwin.moneymanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.data.repository.HomeRepository
import com.alwin.moneymanager.data.repository.HomeSummary
import com.alwin.moneymanager.data.repository.RecentExpenseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: HomeRepository,
) : ViewModel() {

    val summary: StateFlow<HomeSummary> = repository.getHomeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EMPTY_SUMMARY)

    val activeEmis: StateFlow<List<EmiWithProgress>> = repository.getActiveEmis()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentActivity: StateFlow<List<RecentExpenseItem>> = repository.getRecentActivity(RECENT_ACTIVITY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
