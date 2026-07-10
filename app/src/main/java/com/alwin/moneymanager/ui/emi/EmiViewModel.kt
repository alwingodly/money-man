package com.alwin.moneymanager.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.EmiMonthSummary
import com.alwin.moneymanager.data.repository.EmiPeriodTotals
import com.alwin.moneymanager.data.repository.EmiRepository
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.data.repository.periodTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmiViewModel @Inject constructor(
    private val repository: EmiRepository,
) : ViewModel() {

    /** Active loans only — closed ones move to [closedEmiList] to keep this list uncluttered. */
    val emiList: StateFlow<List<EmiWithProgress>> = repository.getAllEmisWithProgress()
        .map { list -> list.filterNot { it.emi.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closedEmiList: StateFlow<List<EmiWithProgress>> = repository.getAllEmisWithProgress()
        .map { list -> list.filter { it.emi.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthSummary: StateFlow<EmiMonthSummary> = repository.getCurrentMonthSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmiMonthSummary(0.0, 0.0))

    /** EMI outgoing bucketed by month and year (every loan's whole schedule), for the totals view. */
    val periodTotals: StateFlow<EmiPeriodTotals> = repository.getAllEmisWithProgress()
        .map { it.periodTotals() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmiPeriodTotals(emptyList(), emptyList()))

    fun addEmi(form: EmiFormResult) {
        viewModelScope.launch {
            repository.addEmi(
                name = form.name,
                monthlyAmount = form.monthlyAmount,
                totalMonths = form.totalMonths,
                startDateMillis = form.startDateMillis,
                notes = form.notes,
                notificationEnabled = form.notificationEnabled,
                reminderDaysBefore = form.reminderDaysBefore,
                loanAmount = form.loanAmount,
                frequency = form.frequency,
                offDaysMask = form.offDaysMask,
                intervalDays = form.intervalDays,
            )
        }
    }

    fun deleteEmi(emiWithProgress: EmiWithProgress) {
        viewModelScope.launch { repository.deleteEmi(emiWithProgress.emi) }
    }
}
