package com.alwin.moneymanager.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.EmiMonthSummary
import com.alwin.moneymanager.data.repository.EmiRepository
import com.alwin.moneymanager.data.repository.EmiWithProgress
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

    fun addEmi(form: EmiFormResult) {
        viewModelScope.launch {
            repository.addEmi(
                name = form.name,
                monthlyAmount = form.monthlyAmount,
                totalMonths = form.totalMonths,
                startDateMillis = form.startDateMillis,
                endDateMillis = form.endDateMillis,
                notes = form.notes,
                notificationEnabled = form.notificationEnabled,
                reminderDaysBefore = form.reminderDaysBefore,
                loanAmount = form.loanAmount,
            )
        }
    }

    fun deleteEmi(emiWithProgress: EmiWithProgress) {
        viewModelScope.launch { repository.deleteEmi(emiWithProgress.emi) }
    }
}
