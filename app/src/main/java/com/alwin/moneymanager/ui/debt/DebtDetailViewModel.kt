package com.alwin.moneymanager.ui.debt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.DebtEntry
import com.alwin.moneymanager.data.repository.DebtRepository
import com.alwin.moneymanager.data.repository.DebtWithProgress
import com.alwin.moneymanager.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DebtRepository,
) : ViewModel() {

    private val debtId: Long = checkNotNull(savedStateHandle[Destination.DebtDetail.ARG_DEBT_ID])

    val debtWithProgress: StateFlow<DebtWithProgress?> = repository.getDebtWithProgress(debtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** "You gave" — pushes the balance toward them owing you. */
    fun addGiven(amount: Double, dateMillis: Long) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch { repository.addEntry(current.debt, isGiven = true, amount = amount, dateMillis = dateMillis) }
    }

    /** "You got" — pushes the balance toward you owing them. */
    fun addGot(amount: Double, dateMillis: Long) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch { repository.addEntry(current.debt, isGiven = false, amount = amount, dateMillis = dateMillis) }
    }

    /** One-tap settle: records the balancing entry and hands it back so the screen can offer undo. */
    fun settleUp(onSettled: (DebtEntry) -> Unit) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch {
            val entry = repository.settle(current.debt) ?: return@launch
            onSettled(entry)
        }
    }

    fun deleteEntry(entry: DebtEntry) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch { repository.deleteEntry(current.debt, entry) }
    }

    fun restoreEntry(entry: DebtEntry) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch { repository.restoreEntry(current.debt, entry) }
    }

    fun updateDebt(form: DebtFormResult) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch {
            repository.updateDebt(
                debt = current.debt,
                personName = form.personName,
                note = form.note,
                dueDateMillis = form.dueDateMillis,
                notificationEnabled = form.notificationEnabled,
            )
        }
    }

    fun deleteDebt(onDeleted: () -> Unit) {
        val current = debtWithProgress.value ?: return
        viewModelScope.launch {
            repository.deleteDebt(current.debt)
            onDeleted()
        }
    }
}
