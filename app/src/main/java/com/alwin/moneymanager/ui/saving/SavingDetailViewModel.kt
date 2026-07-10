package com.alwin.moneymanager.ui.saving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.local.entity.SavingContribution
import com.alwin.moneymanager.data.repository.SavingRepository
import com.alwin.moneymanager.data.repository.SavingWithProgress
import com.alwin.moneymanager.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SavingRepository,
) : ViewModel() {

    private val savingId: Long = checkNotNull(savedStateHandle[Destination.SavingDetail.ARG_SAVING_ID])

    val savingWithProgress: StateFlow<SavingWithProgress?> = repository.getSavingWithProgress(savingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addContribution(amount: Double, dateMillis: Long) {
        val current = savingWithProgress.value ?: return
        viewModelScope.launch { repository.addContribution(current.saving, amount, dateMillis) }
    }

    fun deleteContribution(contribution: SavingContribution) {
        val current = savingWithProgress.value ?: return
        viewModelScope.launch { repository.deleteContribution(current.saving, contribution) }
    }

    fun restoreContribution(contribution: SavingContribution) {
        val current = savingWithProgress.value ?: return
        viewModelScope.launch { repository.restoreContribution(current.saving, contribution) }
    }

    fun updateSaving(form: SavingFormResult) {
        val current = savingWithProgress.value ?: return
        viewModelScope.launch {
            repository.updateSaving(current.saving, name = form.name, targetAmount = form.targetAmount, note = form.note)
        }
    }

    fun deleteSaving(onDeleted: () -> Unit) {
        val current = savingWithProgress.value ?: return
        viewModelScope.launch {
            repository.deleteSaving(current.saving)
            onDeleted()
        }
    }
}
