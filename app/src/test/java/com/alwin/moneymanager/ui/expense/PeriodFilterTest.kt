package com.alwin.moneymanager.ui.expense

import com.alwin.moneymanager.data.local.entity.Expense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Boundary behaviour of the day/month/year window filtering used by the Expenses screen. */
class PeriodFilterTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Millis at [hour]:00 on the given calendar day, in the same zone filterByPeriod uses. */
    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()

    private fun expense(millis: Long) = Expense(categoryId = 1, amount = 10.0, dateMillis = millis)

    private val reference = at(2026, 6, 15)

    @Test
    fun all_returnsEverythingUnfiltered() {
        val list = listOf(expense(at(2020, 1, 1)), expense(at(2030, 1, 1)))
        assertEquals(list, filterByPeriod(list, PeriodFilter(PeriodGranularity.ALL, reference)))
    }

    @Test
    fun day_keepsOnlyThatCalendarDay() {
        val inDay = expense(at(2026, 6, 15, hour = 1))
        val previousDay = expense(at(2026, 6, 14, hour = 23))
        val nextDay = expense(at(2026, 6, 16, hour = 0))
        val result = filterByPeriod(
            listOf(inDay, previousDay, nextDay),
            PeriodFilter(PeriodGranularity.DAY, reference),
        )
        assertEquals(listOf(inDay), result)
    }

    @Test
    fun month_includesFirstAndLastDay_excludesAdjacentMonths() {
        val firstOfMonth = expense(at(2026, 6, 1))
        val lastOfMonth = expense(at(2026, 6, 30))
        val previousMonth = expense(at(2026, 5, 31))
        val nextMonth = expense(at(2026, 7, 1))
        val result = filterByPeriod(
            listOf(firstOfMonth, lastOfMonth, previousMonth, nextMonth),
            PeriodFilter(PeriodGranularity.MONTH, reference),
        )
        assertEquals(listOf(firstOfMonth, lastOfMonth), result)
    }

    @Test
    fun year_includesWholeYear_excludesNeighbours() {
        val january = expense(at(2026, 1, 1))
        val december = expense(at(2026, 12, 31))
        val previousYear = expense(at(2025, 12, 31))
        val nextYear = expense(at(2027, 1, 1))
        val result = filterByPeriod(
            listOf(january, december, previousYear, nextYear),
            PeriodFilter(PeriodGranularity.YEAR, reference),
        )
        assertEquals(listOf(january, december), result)
    }

    @Test
    fun shiftPeriod_month_movesReferenceByOneMonth_atStartOfDay() {
        val shifted = shiftPeriod(PeriodFilter(PeriodGranularity.MONTH, reference), delta = 1)
        val expected = LocalDate.of(2026, 7, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expected, shifted)
    }
}
