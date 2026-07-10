package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.DebtDao
import com.alwin.moneymanager.data.local.entity.Debt
import com.alwin.moneymanager.data.local.entity.DebtEntry
import com.alwin.moneymanager.reminder.DebtReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

/** A person's account plus its full ledger. The net of the entries decides who owes whom. */
data class DebtWithProgress(
    val debt: Debt,
    val entries: List<DebtEntry>,
) {
    val totalGiven: Double get() = entries.filter { it.isGiven }.sumOf { it.amount }
    val totalGot: Double get() = entries.filterNot { it.isGiven }.sumOf { it.amount }

    /** Signed net: positive = they owe you, negative = you owe them, 0 = settled. */
    val netBalance: Double get() = totalGiven - totalGot

    /** Direction of the current balance. Null when settled (net 0). */
    val isOwedToMe: Boolean? get() = when {
        netBalance > 0.0 -> true
        netBalance < 0.0 -> false
        else -> null
    }

    /** Amount owing, unsigned (what the row/overview shows). */
    val outstanding: Double get() = abs(netBalance)

    val isSettled: Boolean get() = entries.isNotEmpty() && abs(netBalance) < 0.005
}

/** Totals split by which way each person's net balance points, for the summary + Home tiles. */
data class DebtSummary(
    val totalToCollect: Double, // Σ of balances where they owe you
    val totalToPay: Double,     // Σ of |balances| where you owe them
)

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DebtRepository @Inject constructor(
    private val debtDao: DebtDao,
    private val reminderScheduler: DebtReminderScheduler,
) {
    // One cursor for every account + one for every entry, joined in memory — instead of opening a
    // separate Room Flow per debt (an N+1 that flatMapLatest also tore down and rebuilt in full on
    // every change). Two stable cursors regardless of how many people are in the ledger keeps the
    // list light on low-end devices as it grows.
    fun getAllDebtsWithProgress(): Flow<List<DebtWithProgress>> =
        combine(debtDao.getAllDebts(), debtDao.getAllEntries()) { debts, entries ->
            val entriesByDebt = entries.groupBy { it.debtId }
            debts.map { debt -> DebtWithProgress(debt, entriesByDebt[debt.id].orEmpty()) }
        }

    fun getDebtWithProgress(debtId: Long): Flow<DebtWithProgress?> =
        debtDao.getDebtById(debtId).flatMapLatest { debt ->
            if (debt == null) flowOf(null) else debtWithProgressFlow(debt)
        }

    fun getSummary(): Flow<DebtSummary> = getAllDebtsWithProgress().map { debts ->
        DebtSummary(
            totalToCollect = debts.filter { it.netBalance > 0 }.sumOf { it.netBalance },
            totalToPay = debts.filter { it.netBalance < 0 }.sumOf { -it.netBalance },
        )
    }

    private fun debtWithProgressFlow(debt: Debt): Flow<DebtWithProgress> =
        debtDao.getEntriesForDebt(debt.id).map { entries -> DebtWithProgress(debt, entries) }

    /**
     * Records a transaction for a person. If an account for this person already exists (any
     * direction — there's one per person now), the entry is appended to it; otherwise a new account
     * is created. [isGiven] true = you gave money, false = you got money.
     */
    suspend fun addOrRecord(
        personName: String,
        isGiven: Boolean,
        amount: Double,
        note: String,
        dueDateMillis: Long?,
        notificationEnabled: Boolean,
        dateMillis: Long,
    ) {
        val existing = debtDao.findAccount(personName)
        val account = if (existing != null) {
            existing.copy(dueDateMillis = dueDateMillis, notificationEnabled = notificationEnabled)
                .also { debtDao.updateDebt(it) }
        } else {
            val created = Debt(
                personName = personName,
                note = note,
                createdDateMillis = dateMillis,
                dueDateMillis = dueDateMillis,
                notificationEnabled = notificationEnabled,
                isSettled = false,
            )
            created.copy(id = debtDao.insertDebt(created))
        }
        debtDao.insertEntry(
            DebtEntry(debtId = account.id, isGiven = isGiven, amount = amount, dateMillis = dateMillis, note = note),
        )
        refreshSettledState(account)
    }

    /** Adds an entry to an existing account from its ledger screen. */
    suspend fun addEntry(debt: Debt, isGiven: Boolean, amount: Double, dateMillis: Long) {
        debtDao.insertEntry(
            DebtEntry(debtId = debt.id, isGiven = isGiven, amount = amount, dateMillis = dateMillis),
        )
        refreshSettledState(debt)
    }

    /**
     * One-tap "settle up": records a single balancing entry for the exact outstanding amount so the
     * account nets to zero. Reads the live balance (not a possibly-stale UI value) and no-ops if it's
     * already settled. A positive balance (they owe you) is cleared by a "got" entry; a negative one
     * (you owe them) by a "given" entry. Returns the inserted entry so the caller can offer undo, or
     * null if there was nothing to settle.
     */
    suspend fun settle(debt: Debt): DebtEntry? {
        val balance = debtDao.getBalance(debt.id)
        if (abs(balance) < 0.005) return null
        val entry = DebtEntry(
            debtId = debt.id,
            isGiven = balance < 0.0,
            amount = abs(balance),
            dateMillis = System.currentTimeMillis(),
            note = "Settled up",
        )
        val id = debtDao.insertEntry(entry)
        refreshSettledState(debt)
        return entry.copy(id = id)
    }

    suspend fun updateDebt(
        debt: Debt,
        personName: String,
        note: String,
        dueDateMillis: Long?,
        notificationEnabled: Boolean,
    ) {
        val updated = debt.copy(
            personName = personName,
            note = note,
            dueDateMillis = dueDateMillis,
            notificationEnabled = notificationEnabled,
        )
        debtDao.updateDebt(updated)
        reminderScheduler.scheduleReminder(updated)
    }

    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
        reminderScheduler.cancelReminder(debt.id)
    }

    suspend fun deleteEntry(debt: Debt, entry: DebtEntry) {
        debtDao.deleteEntry(entry)
        refreshSettledState(debt)
    }

    suspend fun restoreEntry(debt: Debt, entry: DebtEntry) {
        debtDao.insertEntry(entry.copy(id = 0))
        refreshSettledState(debt)
    }

    private suspend fun refreshSettledState(debt: Debt) {
        val settled = abs(debtDao.getBalance(debt.id)) < 0.005
        val current = if (settled != debt.isSettled) {
            debt.copy(isSettled = settled).also { debtDao.updateDebt(it) }
        } else {
            debt
        }
        reminderScheduler.scheduleReminder(current)
    }
}
