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

    fun markNextMonthPaid(onCompleted: () -> Unit) {
        val current = emiWithProgress.value ?: return
        if (current.paidMonths >= current.emi.totalMonths) return
        viewModelScope.launch {
            val justCompleted = repository.markNextMonthPaid(current.emi, current.paidMonths)
            if (justCompleted) onCompleted()
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
                endDateMillis = form.endDateMillis,
                notes = form.notes,
                notificationEnabled = form.notificationEnabled,
                reminderDaysBefore = form.reminderDaysBefore,
                loanAmount = form.loanAmount,
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
