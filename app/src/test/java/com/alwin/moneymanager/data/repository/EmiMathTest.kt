package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.util.addMonths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure math on [EmiWithProgress] plus the due-date-based range aggregations used by Home. */
class EmiMathTest {

    private val start = 1_700_000_000_000L // fixed instant so month arithmetic is deterministic

    private fun emi(months: Int = 12, monthly: Double = 1000.0, loan: Double = 0.0) = Emi(
        id = 1,
        name = "Car loan",
        monthlyAmount = monthly,
        totalMonths = months,
        startDateMillis = start,
        loanAmount = loan,
    )

    private fun payments(count: Int) =
        (1..count).map { EmiPayment(emiId = 1, monthNumber = it, paidDateMillis = start) }

    @Test
    fun paidAndRemaining_countsAndAmounts() {
        val p = EmiWithProgress(emi(months = 12), payments(5))
        assertEquals(5, p.paidMonths)
        assertEquals(7, p.remainingMonths)
        assertEquals(7000.0, p.remainingAmount, EPSILON)
        assertEquals(12000.0, p.totalPayable, EPSILON)
    }

    @Test
    fun progress_isPaidOverTotal() {
        assertEquals(0.25f, EmiWithProgress(emi(months = 4), payments(1)).progressPercent, EPSILON_F)
    }

    @Test
    fun progress_zeroMonths_doesNotDivideByZero() {
        assertEquals(0f, EmiWithProgress(emi(months = 0), emptyList()).progressPercent, EPSILON_F)
    }

    @Test
    fun totalInterest_isNull_whenPrincipalUnknown() {
        // loanAmount 0 means "not entered", not a genuine zero-interest loan.
        assertNull(EmiWithProgress(emi(loan = 0.0), emptyList()).totalInterest)
    }

    @Test
    fun totalInterest_isPayableMinusPrincipal() {
        val p = EmiWithProgress(emi(months = 12, monthly = 1000.0, loan = 10000.0), emptyList())
        assertEquals(2000.0, p.totalInterest!!, EPSILON) // 12 * 1000 - 10000
    }

    @Test
    fun nextDueDate_isInstallmentAtPaidIndex() {
        val p = EmiWithProgress(emi(months = 12), payments(3))
        assertEquals(addMonths(start, 3), p.nextDueDateMillis)
    }

    @Test
    fun nextDueDate_isNull_whenFullyPaid() {
        assertNull(EmiWithProgress(emi(months = 12), payments(12)).nextDueDateMillis)
    }

    @Test
    fun paidAmountInRange_countsByInstallmentDueDate_notPaidDate() {
        // Installment m is due at addMonths(start, m-1). Paid months 1..3 → due at start, +1, +2.
        val progress = listOf(EmiWithProgress(emi(months = 12, monthly = 1000.0), payments(3)))
        // Window covering only installment 2's due date.
        val amount = progress.paidAmountInRange(addMonths(start, 1), addMonths(start, 2))
        assertEquals(1000.0, amount, EPSILON)
    }

    @Test
    fun dueAmountInRange_countsNextUnpaidInstallmentOnlyInsideWindow() {
        val progress = listOf(EmiWithProgress(emi(months = 12, monthly = 1000.0), payments(2)))
        // Next unpaid installment is due at addMonths(start, 2).
        assertEquals(1000.0, progress.dueAmountInRange(addMonths(start, 2), addMonths(start, 3)), EPSILON)
        assertEquals(0.0, progress.dueAmountInRange(start, addMonths(start, 1)), EPSILON)
    }

    private companion object {
        const val EPSILON = 0.0001
        const val EPSILON_F = 0.0001f
    }
}
