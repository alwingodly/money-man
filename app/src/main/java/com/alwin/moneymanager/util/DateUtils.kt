package com.alwin.moneymanager.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Number of calendar months from [startMillis] to [endMillis], inclusive of both ends, counting
 * by (year, month) only — day-of-month is deliberately ignored.
 *
 * `ChronoUnit.MONTHS.between` (a day-aware diff) floors whenever the end date's day-of-month is
 * earlier than the start date's (e.g. start day 3, end day 2), undercounting by a full month. For
 * an EMI, "start July, end February" should always be the same total months regardless of which
 * day of the month was picked — this is why the count is done in (year*12 + month) space.
 */
fun calendarMonthsInclusive(startMillis: Long, endMillis: Long): Int {
    val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val startYearMonth = start.year * 12 + start.monthValue
    val endYearMonth = end.year * 12 + end.monthValue
    return endYearMonth - startYearMonth + 1
}

fun addMonths(millis: Long, months: Int): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).plusMonths(months.toLong())
        .toInstant().toEpochMilli()

fun addDays(millis: Long, days: Int): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).plusDays(days.toLong())
        .toInstant().toEpochMilli()

fun addWeeks(millis: Long, weeks: Int): Long = addDays(millis, weeks * 7)

fun subtractDays(millis: Long, days: Int): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).minusDays(days.toLong())
        .toInstant().toEpochMilli()

private val monthAbbrevFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

/** Two-line "Jul\n2025" style label — used on compact UI like the EMI month grid. Uses the full
 * four-digit year; a two-digit year ("26") reads like a day-of-month. */
fun shortMonthYearLabel(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${date.format(monthAbbrevFormatter)}\n${date.year}"
}

/** "Good morning"/"Good afternoon"/"Good evening" based on the current local time of day. */
fun timeOfDayGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
