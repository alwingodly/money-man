package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.entity.Debt
import com.alwin.moneymanager.data.local.entity.DebtEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure math on [DebtWithProgress]: net balance drives who-owes-whom, the direction, and settled. */
class DebtMathTest {

    private fun debt() = Debt(id = 1, personName = "Ammu", createdDateMillis = 0L)
    private fun given(amount: Double) = DebtEntry(debtId = 1, isGiven = true, amount = amount, dateMillis = 0L)
    private fun got(amount: Double) = DebtEntry(debtId = 1, isGiven = false, amount = amount, dateMillis = 0L)

    @Test
    fun givenMoreThanGot_theyOweYou() {
        val p = DebtWithProgress(debt(), listOf(given(1000.0), got(300.0)))
        assertEquals(700.0, p.netBalance, EPSILON)
        assertEquals(700.0, p.outstanding, EPSILON)
        assertEquals(true, p.isOwedToMe)
        assertFalse(p.isSettled)
    }

    @Test
    fun gotMoreThanGiven_youOweThem_andOutstandingIsUnsigned() {
        val p = DebtWithProgress(debt(), listOf(got(500.0)))
        assertEquals(-500.0, p.netBalance, EPSILON)
        assertEquals(500.0, p.outstanding, EPSILON) // shown unsigned
        assertEquals(false, p.isOwedToMe)
    }

    @Test
    fun netZero_withEntries_isSettled_andDirectionIsNull() {
        val p = DebtWithProgress(debt(), listOf(given(500.0), got(500.0)))
        assertEquals(0.0, p.netBalance, EPSILON)
        assertTrue(p.isSettled)
        assertNull(p.isOwedToMe)
    }

    @Test
    fun emptyAccount_isNotSettled() {
        val p = DebtWithProgress(debt(), emptyList())
        assertFalse(p.isSettled) // "no entries" is not the same as "paid off"
        assertEquals(0.0, p.outstanding, EPSILON)
        assertNull(p.isOwedToMe)
    }

    @Test
    fun nearZeroBalance_absorbsFloatingPointDrift() {
        // 0.1 + 0.2 - 0.3 != 0 exactly in IEEE-754; the < 0.005 threshold treats it as settled.
        val p = DebtWithProgress(debt(), listOf(given(0.1), given(0.2), got(0.3)))
        assertTrue(p.isSettled)
    }

    @Test
    fun balanceAboveEpsilon_isNotSettled() {
        val p = DebtWithProgress(debt(), listOf(given(500.0), got(490.0)))
        assertFalse(p.isSettled)
        assertEquals(10.0, p.outstanding, EPSILON)
    }

    @Test
    fun totals_areSplitByDirection() {
        val p = DebtWithProgress(debt(), listOf(given(100.0), given(50.0), got(30.0)))
        assertEquals(150.0, p.totalGiven, EPSILON)
        assertEquals(30.0, p.totalGot, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.0001
    }
}
