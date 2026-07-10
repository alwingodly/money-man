package com.alwin.moneymanager.util

import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Due-date scheduling for the three EMI frequencies, including daily off-day skipping. */
class EmiScheduleTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millis(date: LocalDate, hour: Int = 9): Long =
        date.atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()

    private fun dateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private fun emi(
        frequency: EmiFrequency,
        start: LocalDate,
        offDaysMask: Int = 0,
        total: Int = 10,
    ) = Emi(
        name = "Loan",
        monthlyAmount = 100.0,
        totalMonths = total,
        startDateMillis = millis(start),
        frequency = frequency,
        offDaysMask = offDaysMask,
    )

    private val monday = LocalDate.of(2026, 1, 5) // a Monday
    private val sunday = LocalDate.of(2026, 1, 4) // a Sunday

    @Test
    fun monthly_addsCalendarMonths() {
        val e = emi(EmiFrequency.MONTHLY, monday)
        assertEquals(LocalDate.of(2026, 4, 5), dateOf(installmentDueDate(e, 3)))
    }

    @Test
    fun weekly_addsSevenDaysPerInstallment_stayingOnTheSameWeekday() {
        val e = emi(EmiFrequency.WEEKLY, monday)
        val due = dateOf(installmentDueDate(e, 3))
        assertEquals(LocalDate.of(2026, 1, 26), due) // +21 days
        assertEquals(DayOfWeek.MONDAY, due.dayOfWeek)
    }

    @Test
    fun daily_noOffDays_stepsEveryDay() {
        val e = emi(EmiFrequency.DAILY, monday)
        assertEquals(LocalDate.of(2026, 1, 10), dateOf(installmentDueDate(e, 5)))
    }

    @Test
    fun daily_skipsOffWeekday_pushingScheduleOut() {
        // Sunday off. From Mon Jan 5: indices 0..5 = Jan 5..10, Sun Jan 11 skipped, index 6 = Mon Jan 12.
        val e = emi(EmiFrequency.DAILY, monday, offDaysMask = offDaysMaskOf(setOf(DayOfWeek.SUNDAY)))
        assertEquals(LocalDate.of(2026, 1, 12), dateOf(installmentDueDate(e, 6)))
    }

    @Test
    fun daily_neverLandsOnAnOffDay() {
        val off = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val e = emi(EmiFrequency.DAILY, monday, offDaysMask = offDaysMaskOf(off))
        (0 until 20).forEach { i ->
            assertFalse(dateOf(installmentDueDate(e, i)).dayOfWeek in off)
        }
    }

    @Test
    fun daily_startingOnOffDay_firstInstallmentMovesToWorkingDay() {
        val e = emi(EmiFrequency.DAILY, sunday, offDaysMask = offDaysMaskOf(setOf(DayOfWeek.SUNDAY)))
        assertEquals(LocalDate.of(2026, 1, 5), dateOf(installmentDueDate(e, 0))) // Sunday → Monday
    }

    @Test
    fun daily_allWeekdaysOff_fallsBackToEveryDay_noInfiniteLoop() {
        val allOff = offDaysMaskOf(DayOfWeek.entries.toSet())
        val e = emi(EmiFrequency.DAILY, monday, offDaysMask = allOff)
        assertEquals(LocalDate.of(2026, 1, 10), dateOf(installmentDueDate(e, 5)))
    }

    @Test
    fun endDate_isLastInstallmentDueDate() {
        val e = emi(EmiFrequency.WEEKLY, monday, total = 10)
        assertEquals(installmentDueDate(e, 9), emiEndDate(e, 10))
    }

    @Test
    fun offDaysMask_roundTrips() {
        val days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertEquals(days, offDaysOf(offDaysMaskOf(days)))
    }

    @Test
    fun isOffDay_readsTheMask() {
        val mask = offDaysMaskOf(setOf(DayOfWeek.SUNDAY))
        assertTrue(isOffDay(DayOfWeek.SUNDAY, mask))
        assertFalse(isOffDay(DayOfWeek.MONDAY, mask))
    }

    @Test
    fun isPaidLate_comparesByCalendarDay() {
        val due = millis(LocalDate.of(2026, 6, 15), hour = 9)
        // Same day (even later in the day) is on time; the next day is late; earlier is not.
        assertFalse(isPaidLate(due, millis(LocalDate.of(2026, 6, 15), hour = 23)))
        assertTrue(isPaidLate(due, millis(LocalDate.of(2026, 6, 16), hour = 0)))
        assertFalse(isPaidLate(due, millis(LocalDate.of(2026, 6, 14), hour = 23)))
    }
}
