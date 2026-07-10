package com.alwin.moneymanager.ui.emi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.EmiRepository
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmiRepository,
) : ViewModel() {

    private val emiId: Long = checkNotNull(savedStateHandle[Destination.EmiDetail.ARG_EMI_ID])

    val emiWithProgress: StateFlow<EmiWithProgress?> = repository.getEmiWithProgress(emiId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Blocks a second "mark paid" from launching while the first is still writing — the StateFlow
    // hasn't re-emitted the new paidMonths yet, so without this both taps would compute the same
    // next installment. The DB unique index is the ultimate backstop; this just avoids the wasted
    // round-trip and any UI flicker.
    private var isMarkingPaid = false

    fun markNextMonthPaid(penaltyAmount: Double = 0.0, onCompleted: () -> Unit) {
        val current = emiWithProgress.value ?: return
        if (current.paidMonths >= current.emi.totalMonths) return
        if (isMarkingPaid) return
        isMarkingPaid = true
        viewModelScope.launch {
            try {
                val justCompleted = repository.markNextMonthPaid(current.emi, current.paidMonths, penaltyAmount)
                if (justCompleted) onCompleted()
            } finally {
                isMarkingPaid = false
            }
        }
    }

    fun updateEmi(form: EmiFormResult) {
        val current = emiWithProgress.value ?: return
        viewModelScope.launch {
            repository.updateEmiDetails(
                emi = current.emi,
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

    fun undoLastPayment() {
        val current = emiWithProgress.value ?: return
        if (current.paidMonths == 0) return
        viewModelScope.launch { repository.undoLastPayment(current.emi) }
    }

    fun deleteEmi(onDeleted: () -> Unit) {
        val current = emiWithProgress.value ?: return
        viewModelScope.launch {
            repository.deleteEmi(current.emi)
            onDeleted()
        }
    }
}
