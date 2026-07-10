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
import kotlinx.coroutines.flow.map
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
    debtToCollect = 0.0,
    debtToPay = 0.0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: HomeRepository,
) : ViewModel() {

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

    // True until the dashboard's first data arrives, so Home shows a spinner instead of briefly
    // flashing the "Welcome" empty state (which looks identical to a genuinely empty account) to
    // a returning user while Room loads.
    val isLoading: StateFlow<Boolean> = summaryState
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
