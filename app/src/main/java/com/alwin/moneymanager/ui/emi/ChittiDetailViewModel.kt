package com.alwin.moneymanager.ui.emi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.ChittiFrequency
import com.alwin.moneymanager.data.local.entity.ChittiPayment
import com.alwin.moneymanager.data.repository.ChittiRepository
import com.alwin.moneymanager.data.repository.ChittiWithProgress
import com.alwin.moneymanager.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ChittiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChittiRepository,
) : ViewModel() {

    private val chittiId: Long = checkNotNull(savedStateHandle[Destination.ChittiDetail.ARG_CHITTI_ID])

    val chittiWithProgress: StateFlow<ChittiWithProgress?> =
        repository.getChittiWithProgress(chittiId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addPayment(amount: Double, note: String) {
        viewModelScope.launch {
            val current = chittiWithProgress.value ?: return@launch
            val nextMonth = current.paidMonths + 1
            repository.addPayment(
                ChittiPayment(
                    chittiId = chittiId,
                    monthNumber = nextMonth,
                    amount = amount,
                    paidDateMillis = Instant.now().toEpochMilli(),
                    note = note,
                )
            )
            if (nextMonth >= current.chitti.totalMonths) {
                repository.updateChitti(current.chitti.copy(isCompleted = true))
            }
        }
    }

    fun markReceived(amount: Double) {
        viewModelScope.launch {
            val current = chittiWithProgress.value ?: return@launch
            repository.updateChitti(
                current.chitti.copy(
                    receivedAmount = amount,
                    receivedMonthNumber = current.paidMonths,
                    receivedDateMillis = Instant.now().toEpochMilli(),
                )
            )
        }
    }

    fun updateChitti(name: String, notes: String, frequency: ChittiFrequency) {
        viewModelScope.launch {
            val current = chittiWithProgress.value ?: return@launch
            repository.updateChitti(
                current.chitti.copy(
                    name = name,
                    notes = notes,
                    frequency = frequency,
                )
            )
        }
    }

    fun deleteChitti() {
        viewModelScope.launch {
            val current = chittiWithProgress.value ?: return@launch
            repository.deleteChitti(current.chitti)
        }
    }

    fun updatePayment(payment: ChittiPayment) {
        viewModelScope.launch {
            repository.updatePayment(payment)
        }
    }

    fun deletePayment(payment: ChittiPayment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }
}
