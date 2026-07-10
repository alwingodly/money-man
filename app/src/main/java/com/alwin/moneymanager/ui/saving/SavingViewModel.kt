package com.alwin.moneymanager.ui.saving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.SavingRepository
import com.alwin.moneymanager.data.repository.SavingWithProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingViewModel @Inject constructor(
    private val repository: SavingRepository,
) : ViewModel() {

    private val allSavings: StateFlow<List<SavingWithProgress>?> =
        repository.getAllSavingsWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savings: StateFlow<List<SavingWithProgress>> = allSavings
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSaved: StateFlow<Double> = allSavings
        .map { list -> list.orEmpty().sumOf { it.totalSaved } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isLoading: StateFlow<Boolean> = allSavings
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun addSaving(form: SavingFormResult) {
        viewModelScope.launch {
            repository.addSaving(name = form.name, targetAmount = form.targetAmount, note = form.note)
        }
    }
}
