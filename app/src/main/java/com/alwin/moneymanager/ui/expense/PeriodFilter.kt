package com.alwin.moneymanager.ui.expense

import com.alwin.moneymanager.data.local.entity.Expense
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

enum class PeriodGranularity { ALL, DAY, MONTH, YEAR }

data class PeriodFilter(
    val granularity: PeriodGranularity,
    val referenceMillis: Long = System.currentTimeMillis(),
)

private val zone: ZoneId = ZoneId.systemDefault()
private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

fun periodLabel(filter: PeriodFilter): String {
    val date = Instant.ofEpochMilli(filter.referenceMillis).atZone(zone).toLocalDate()
    return when (filter.granularity) {
        PeriodGranularity.ALL -> "All time"
        PeriodGranularity.DAY -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(filter.referenceMillis))
        PeriodGranularity.MONTH -> date.format(monthYearFormatter)
        PeriodGranularity.YEAR -> date.year.toString()
    }
}

fun shiftPeriod(filter: PeriodFilter, delta: Int): Long {
    val date = Instant.ofEpochMilli(filter.referenceMillis).atZone(zone).toLocalDate()
    val shifted = when (filter.granularity) {
        PeriodGranularity.ALL -> date
        PeriodGranularity.DAY -> date.plusDays(delta.toLong())
        PeriodGranularity.MONTH -> date.plusMonths(delta.toLong())
        PeriodGranularity.YEAR -> date.plusYears(delta.toLong())
    }
    return shifted.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun filterByPeriod(expenses: List<Expense>, filter: PeriodFilter): List<Expense> {
    if (filter.granularity == PeriodGranularity.ALL) return expenses
    val date = Instant.ofEpochMilli(filter.referenceMillis).atZone(zone).toLocalDate()
    val (start, endExclusive) = when (filter.granularity) {
        PeriodGranularity.DAY -> date to date.plusDays(1)
        PeriodGranularity.MONTH -> date.withDayOfMonth(1) to date.withDayOfMonth(1).plusMonths(1)
        PeriodGranularity.YEAR -> date.withDayOfYear(1) to date.withDayOfYear(1).plusYears(1)
        PeriodGranularity.ALL -> return expenses
    }
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    return expenses.filter { it.dateMillis in startMillis until endMillis }
}

private val dayHeaderFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

fun dayHeaderLabel(date: LocalDate): String = date.format(dayHeaderFormatter)

/** [start, endExclusive) millis range covering the given calendar day. */
fun dayRange(date: LocalDate): Pair<Long, Long> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}

/** Groups already date-sorted (descending) expenses into per-day buckets, preserving order. */
fun groupExpensesByDay(expenses: List<Expense>): List<Pair<LocalDate, List<Expense>>> =
    expenses.groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }.toList()
