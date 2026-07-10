package com.alwin.moneymanager.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.DebtRepository
import com.alwin.moneymanager.data.repository.DebtSummary
import com.alwin.moneymanager.data.repository.DebtWithProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val repository: DebtRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Collected once and shared by every derived flow below (list, summary, names, loading) so the
    // underlying DB query runs a single time instead of once per consumer. null = not loaded yet,
    // which drives the loading spinner without a second subscription just to detect first emission.
    private val allDebts: StateFlow<List<DebtWithProgress>?> =
        repository.getAllDebtsWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Active (unsettled) debts filtered by the search box. Settled ones move to the History
     * screen so the main ledger stays focused on what still needs action — same split as the EMI
     * module's active list vs. "Closed loans" archive. */
    val debts: StateFlow<List<DebtWithProgress>> = combine(allDebts, _query) { list, q ->
        list.orEmpty().filterNot { it.isSettled }.matching(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<DebtSummary> = allDebts.map { list ->
        val all = list.orEmpty()
        DebtSummary(
            totalToCollect = all.filter { it.netBalance > 0 }.sumOf { it.netBalance },
            totalToPay = all.filter { it.netBalance < 0 }.sumOf { -it.netBalance },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtSummary(0.0, 0.0))

    /** Distinct names already used, offered as suggestions when adding a debt so the same person
     * (e.g. "Ammu") is reused instead of creating a duplicate. */
    val existingNames: StateFlow<List<String>> = allDebts
        .map { list -> list.orEmpty().map { it.debt.personName }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = allDebts
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun addDebt(form: DebtFormResult) {
        viewModelScope.launch {
            repository.addOrRecord(
                personName = form.personName,
                isGiven = form.isGiven,
                amount = form.originalAmount,
                note = form.note,
                dueDateMillis = form.dueDateMillis,
                notificationEnabled = form.notificationEnabled,
                dateMillis = form.transactionDateMillis,
            )
        }
    }
}

/** Case-insensitive filter over person name and note; blank query returns the list unchanged. */
internal fun List<DebtWithProgress>.matching(query: String): List<DebtWithProgress> {
    if (query.isBlank()) return this
    val needle = query.trim().lowercase()
    return filter {
        it.debt.personName.lowercase().contains(needle) || it.debt.note.lowercase().contains(needle)
    }
}
