package com.alwin.moneymanager.util

import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiFrequency
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * The single source of truth for when an EMI installment falls due, generalizing the old
 * monthly-only `addMonths(start, n)`.
 *
 *  - [EmiFrequency.MONTHLY]: installment `index` is due `index` months after the start date.
 *  - [EmiFrequency.WEEKLY]: `index` weeks after the start date (the start already sits on the
 *    chosen weekday, so every installment lands on that weekday).
 *  - [EmiFrequency.DAILY]: the `index`-th *working day* on or after the start date, skipping any
 *    weekdays marked off in [Emi.offDaysMask]. Off days therefore push the schedule outward and
 *    extend the loan's end date, rather than dropping installments.
 *
 * [index] is 0-based (installment 1 is `index = 0`).
 */
fun installmentDueDate(emi: Emi, index: Int): Long = when (emi.frequency) {
    EmiFrequency.MONTHLY -> addMonths(emi.startDateMillis, index)
    EmiFrequency.WEEKLY -> addWeeks(emi.startDateMillis, index)
    EmiFrequency.DAILY -> dailyDueDate(emi.startDateMillis, emi.offDaysMask, index)
    // Every `intervalDays` days from the start — "every 28 days", same weekday if the interval is
    // a multiple of 7. Guard a non-positive interval to avoid all installments collapsing onto day 0.
    EmiFrequency.CUSTOM -> addDays(emi.startDateMillis, index * emi.intervalDays.coerceAtLeast(1))
}

/** Due date of the final installment — i.e. the loan's end date — for a [totalInstallments]-long EMI. */
fun emiEndDate(emi: Emi, totalInstallments: Int): Long =
    if (totalInstallments <= 0) emi.startDateMillis else installmentDueDate(emi, totalInstallments - 1)

/** True if [paidMillis] falls on a calendar day after [dueMillis] — i.e. the installment was paid
 * late. Same-day payment counts as on time. */
fun isPaidLate(dueMillis: Long, paidMillis: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val due = Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalDate()
    val paid = Instant.ofEpochMilli(paidMillis).atZone(zone).toLocalDate()
    return paid.isAfter(due)
}

private fun dailyDueDate(startMillis: Long, offDaysMask: Int, index: Int): Long {
    // Guard against a mask that marks every weekday off (no working day would ever exist): fall
    // back to plain day stepping. The form prevents this, but never trust it to.
    val mask = if (workingDaysExist(offDaysMask)) offDaysMask else 0
    if (mask == 0) return addDays(startMillis, index)

    var dt = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault())
    while (isOffDay(dt.dayOfWeek, mask)) dt = dt.plusDays(1) // move onto the first working day
    var counted = 0
    while (counted < index) {
        dt = dt.plusDays(1)
        if (!isOffDay(dt.dayOfWeek, mask)) counted++
    }
    return dt.toInstant().toEpochMilli()
}

/** Bit `dayOfWeek.value` (Monday=1 … Sunday=7) set means that weekday is an off day. */
fun isOffDay(day: DayOfWeek, offDaysMask: Int): Boolean = (offDaysMask and (1 shl day.value)) != 0

fun offDaysMaskOf(days: Set<DayOfWeek>): Int = days.fold(0) { acc, day -> acc or (1 shl day.value) }

fun offDaysOf(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(mutableSetOf()) { isOffDay(it, mask) }

private fun workingDaysExist(mask: Int): Boolean = DayOfWeek.entries.any { !isOffDay(it, mask) }
