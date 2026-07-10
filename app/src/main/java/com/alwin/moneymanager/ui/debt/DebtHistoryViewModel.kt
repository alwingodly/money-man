package com.alwin.moneymanager.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.DebtRepository
import com.alwin.moneymanager.data.repository.DebtWithProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DebtHistoryViewModel @Inject constructor(
    repository: DebtRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val allDebts = repository.getAllDebtsWithProgress()

    /** Settled debts only, filtered by the history screen's own search box. */
    val settledDebts: StateFlow<List<DebtWithProgress>> = combine(allDebts, _query) { list, q ->
        list.filter { it.isSettled }.matching(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = allDebts
        .map { false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setQuery(value: String) {
        _query.value = value
    }
}
