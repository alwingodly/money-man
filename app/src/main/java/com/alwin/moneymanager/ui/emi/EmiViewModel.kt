package com.alwin.moneymanager.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.Chitti
import com.alwin.moneymanager.data.repository.ChittiRepository
import com.alwin.moneymanager.data.repository.ChittiWithProgress
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
    private val chittiRepository: ChittiRepository,
) : ViewModel() {

    private val allEmis: StateFlow<List<EmiWithProgress>> = repository.getAllEmisWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Active loans only — closed ones move to [closedEmiList] to keep this list uncluttered. */
    val emiList: StateFlow<List<EmiWithProgress>> = allEmis
        .map { list -> list.filterNot { it.emi.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closedEmiList: StateFlow<List<EmiWithProgress>> = allEmis
        .map { list -> list.filter { it.emi.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthSummary: StateFlow<EmiMonthSummary> = repository.getCurrentMonthSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmiMonthSummary(0.0, 0.0))

    /** Computed only when the totals dialog opens — not on every EMI payment. */
    fun periodTotalsSnapshot(): EmiPeriodTotals = allEmis.value.periodTotals()

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

    /** Active chittis only — completed ones are filtered out. */
    val chittiList: StateFlow<List<ChittiWithProgress>> = chittiRepository.getAllChittisWithProgress()
        .map { list -> list.filterNot { it.chitti.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addChitti(form: ChittiForm) {
        viewModelScope.launch {
            chittiRepository.insertChitti(
                Chitti(
                    name = form.name,
                    totalMonths = form.totalMonths,
                    startDateMillis = form.startDateMillis,
                    notes = form.notes,
                    frequency = form.frequency,
                )
            )
        }
    }

    fun deleteChitti(chittiWithProgress: ChittiWithProgress) {
        viewModelScope.launch { chittiRepository.deleteChitti(chittiWithProgress.chitti) }
    }
}
