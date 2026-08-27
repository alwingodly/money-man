package com.alwin.moneymanager.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class SummaryMode { MONTH, YEAR }

/** One period (a month or a year) with its spend total, count, and per-category breakdown. */
data class PeriodSummary(
    val label: String,
    val total: Double,
    val count: Int,
    val sortKey: Long,
    val categoryBreakdown: List<CategoryTotal>,
)

data class CategoryTotal(val categoryId: Long, val categoryName: String, val amount: Double)

@HiltViewModel
class ExpenseSummaryViewModel @Inject constructor(
    repository: ExpenseRepository,
) : ViewModel() {

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val zone: ZoneId = ZoneId.systemDefault()

    private val _mode = MutableStateFlow(SummaryMode.MONTH)
    val mode: StateFlow<SummaryMode> = _mode.asStateFlow()

    private val summaryState: StateFlow<List<PeriodSummary>?> = combine(
        repository.getAllExpenses(),
        repository.getAllCategories(),
        _mode,
    ) { expenses, categories, mode ->
        val nameById = categories.associate { it.id to it.name }
        expenses
            .groupBy { periodKey(it.dateMillis, mode) }
            .map { (key, items) ->
                PeriodSummary(
                    label = periodLabel(key, mode),
                    total = items.sumOf { it.amount },
                    count = items.size,
                    sortKey = key,
                    categoryBreakdown = items
                        .groupBy { it.categoryId }
                        .map { (categoryId, list) ->
                            CategoryTotal(categoryId, nameById[categoryId] ?: "Unknown", list.sumOf { it.amount })
                        }
                        .sortedByDescending { it.amount },
                )
            }
            .sortedByDescending { it.sortKey }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val summaries: StateFlow<List<PeriodSummary>> = summaryState
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSortKey = MutableStateFlow<Long?>(null)

    val selectedPeriod: StateFlow<PeriodSummary?> = combine(summaries, _selectedSortKey) { list, key ->
        list.firstOrNull { it.sortKey == key } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoading: StateFlow<Boolean> = summaryState
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMode(mode: SummaryMode) {
        _mode.value = mode
        _selectedSortKey.value = null
    }

    fun selectPeriod(sortKey: Long) {
        _selectedSortKey.value = sortKey
    }

    /** A comparable key: year*100 + month for months, year for years. */
    private fun periodKey(dateMillis: Long, mode: SummaryMode): Long {
        val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        return when (mode) {
            SummaryMode.MONTH -> date.year * 100L + date.monthValue
            SummaryMode.YEAR -> date.year.toLong()
        }
    }

    private fun periodLabel(key: Long, mode: SummaryMode): String = when (mode) {
        SummaryMode.MONTH -> {
            val year = (key / 100).toInt()
            val month = (key % 100).toInt()
            java.time.YearMonth.of(year, month).atDay(1).format(monthFormatter)
        }
        SummaryMode.YEAR -> key.toString()
    }
}
