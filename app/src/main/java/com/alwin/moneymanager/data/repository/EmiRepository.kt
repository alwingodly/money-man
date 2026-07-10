package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiFrequency
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.reminder.EmiReminderScheduler
import com.alwin.moneymanager.util.emiEndDate
import com.alwin.moneymanager.util.installmentDueDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class EmiWithProgress(
    val emi: Emi,
    val payments: List<EmiPayment>,
) {
    val paidMonths: Int get() = payments.size
    val remainingMonths: Int get() = emi.totalMonths - paidMonths
    val remainingAmount: Double get() = remainingMonths * emi.monthlyAmount
    val progressPercent: Float
        get() = if (emi.totalMonths == 0) 0f else paidMonths.toFloat() / emi.totalMonths

    /** Due date of the next unpaid installment, or null if every month is already paid. */
    val nextDueDateMillis: Long?
        get() = if (paidMonths < emi.totalMonths) installmentDueDate(emi, paidMonths) else null

    val totalPayable: Double get() = emi.monthlyAmount * emi.totalMonths

    /** Total interest over the life of the loan, or null when [Emi.loanAmount] hasn't been
     * entered (0 = unknown, not a real zero-interest loan) — see [Emi.loanAmount]. */
    val totalInterest: Double?
        get() = if (emi.loanAmount > 0) totalPayable - emi.loanAmount else null

    /** Sum of every late-payment fine recorded against this EMI. */
    val totalPenalty: Double get() = payments.sumOf { it.penaltyAmount }

    /** Installment numbers that carry a late-payment penalty (shown red in the grid). */
    val penalizedMonths: Set<Int>
        get() = payments.filter { it.penaltyAmount > 0 }.map { it.monthNumber }.toSet()
}

data class EmiMonthSummary(val paidAmount: Double, val dueAmount: Double)

/** Total EMI outgoing (sum of installment amounts due) for a single calendar month / year. */
data class EmiMonthlyTotal(val year: Int, val month: Int, val amount: Double)
data class EmiYearlyTotal(val year: Int, val amount: Double)

data class EmiPeriodTotals(
    val months: List<EmiMonthlyTotal>, // most recent first
    val years: List<EmiYearlyTotal>,   // most recent first
)

/**
 * Buckets every installment of every EMI by the calendar month/year it falls due, summing the
 * installment amount into each — so daily/weekly loans roll up into their month, and a month with
 * several loans shows the combined outgoing. Backs the "Monthly & yearly totals" view.
 */
fun List<EmiWithProgress>.periodTotals(zone: ZoneId = ZoneId.systemDefault()): EmiPeriodTotals {
    val byMonth = HashMap<Pair<Int, Int>, Double>()
    forEach { ewp ->
        val emi = ewp.emi
        for (index in 0 until emi.totalMonths) {
            val due = Instant.ofEpochMilli(installmentDueDate(emi, index)).atZone(zone)
            val key = due.year to due.monthValue
            byMonth[key] = (byMonth[key] ?: 0.0) + emi.monthlyAmount
        }
    }
    val months = byMonth.entries
        .map { EmiMonthlyTotal(it.key.first, it.key.second, it.value) }
        .sortedWith(compareByDescending<EmiMonthlyTotal> { it.year }.thenByDescending { it.month })
    val years = months.groupBy { it.year }
        .map { (year, list) -> EmiYearlyTotal(year, list.sumOf { it.amount }) }
        .sortedByDescending { it.year }
    return EmiPeriodTotals(months, years)
}

/**
 * Sum of `monthlyAmount` for every installment whose *due date* (not paid date) falls in
 * [startMillis, endMillis) and has already been paid. Due-date based so catching up on
 * backlogged months in one sitting doesn't inflate whichever month you happened to pay in —
 * see `HomeRepository` for the bug this pattern was fixed to avoid.
 */
fun List<EmiWithProgress>.paidAmountInRange(startMillis: Long, endMillis: Long): Double =
    sumOf { emiWithProgress ->
        val count = emiWithProgress.payments.count { payment ->
            val dueDateMillis = installmentDueDate(emiWithProgress.emi, payment.monthNumber - 1)
            dueDateMillis in startMillis until endMillis
        }
        count * emiWithProgress.emi.monthlyAmount
    }

/** Sum of `monthlyAmount` for every EMI whose next *unpaid* installment is due in the range. */
fun List<EmiWithProgress>.dueAmountInRange(startMillis: Long, endMillis: Long): Double =
    sumOf { emiWithProgress ->
        val dueDateMillis = emiWithProgress.nextDueDateMillis
        if (dueDateMillis != null && dueDateMillis in startMillis until endMillis) {
            emiWithProgress.emi.monthlyAmount
        } else {
            0.0
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class EmiRepository @Inject constructor(
    private val emiDao: EmiDao,
    private val reminderScheduler: EmiReminderScheduler,
) {
    // App-scoped: this @Singleton lives for the whole process, so its scope does too. Backs the
    // shared EMI stream below.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // One payments query per loan (an N+1 fan-out) rebuilt from scratch by every collector adds up:
    // Home alone watches this via both the summary and the active-loans preview, and the EMI screen
    // via its list/closed/totals views. shareIn collapses all concurrent collectors onto a single
    // upstream fan-out; WhileSubscribed(5000) tears it down shortly after the last screen leaves,
    // and replay = 1 hands the latest snapshot to the next subscriber without a reload flash.
    private val allEmisWithProgress: Flow<List<EmiWithProgress>> =
        emiDao.getAllEmis()
            .flatMapLatest { emis ->
                if (emis.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(emis.map { emi -> emiWithProgressFlow(emi) }) { it.toList() }
                }
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 1)

    fun getAllEmisWithProgress(): Flow<List<EmiWithProgress>> = allEmisWithProgress

    fun getEmiWithProgress(emiId: Long): Flow<EmiWithProgress?> =
        emiDao.getEmiById(emiId).flatMapLatest { emi ->
            if (emi == null) flowOf(null) else emiWithProgressFlow(emi)
        }

    /** Paid vs. still-due EMI amount for the current calendar month, across all EMIs. */
    fun getCurrentMonthSummary(): Flow<EmiMonthSummary> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return getAllEmisWithProgress().map { emis ->
            EmiMonthSummary(
                paidAmount = emis.paidAmountInRange(monthStart, monthEnd),
                dueAmount = emis.dueAmountInRange(monthStart, monthEnd),
            )
        }
    }

    private fun emiWithProgressFlow(emi: Emi): Flow<EmiWithProgress> =
        emiDao.getPaymentsForEmi(emi.id).map { payments -> EmiWithProgress(emi, payments) }

    suspend fun addEmi(
        name: String,
        monthlyAmount: Double,
        totalMonths: Int,
        startDateMillis: Long,
        notes: String,
        notificationEnabled: Boolean,
        reminderDaysBefore: Int,
        loanAmount: Double,
        frequency: EmiFrequency,
        offDaysMask: Int,
        intervalDays: Int,
    ) {
        val base = Emi(
            name = name,
            monthlyAmount = monthlyAmount,
            totalMonths = totalMonths,
            startDateMillis = startDateMillis,
            notes = notes,
            notificationEnabled = notificationEnabled,
            reminderDaysBefore = reminderDaysBefore,
            loanAmount = loanAmount,
            frequency = frequency,
            offDaysMask = offDaysMask,
            intervalDays = intervalDays,
        )
        // End date derived from the schedule so weekly/daily (and daily off-day skipping) land it
        // on the real final installment instead of the caller having to reproduce that math.
        val emi = base.copy(endDateMillis = emiEndDate(base, totalMonths))
        val id = emiDao.insertEmi(emi)
        reminderScheduler.scheduleReminder(emi.copy(id = id), paidMonths = 0)
    }

    suspend fun updateEmiDetails(
        emi: Emi,
        name: String,
        monthlyAmount: Double,
        totalMonths: Int,
        startDateMillis: Long,
        notes: String,
        notificationEnabled: Boolean,
        reminderDaysBefore: Int,
        loanAmount: Double,
        frequency: EmiFrequency,
        offDaysMask: Int,
        intervalDays: Int,
    ) {
        val base = emi.copy(
            name = name,
            monthlyAmount = monthlyAmount,
            totalMonths = totalMonths,
            startDateMillis = startDateMillis,
            notes = notes,
            notificationEnabled = notificationEnabled,
            reminderDaysBefore = reminderDaysBefore,
            loanAmount = loanAmount,
            frequency = frequency,
            offDaysMask = offDaysMask,
            intervalDays = intervalDays,
        )
        val updated = base.copy(endDateMillis = emiEndDate(base, totalMonths))
        emiDao.updateEmi(updated)
        val paidMonths = emiDao.getPaidMonthCount(updated.id)
        reminderScheduler.scheduleReminder(updated, paidMonths)
    }

    suspend fun deleteEmi(emi: Emi) {
        emiDao.deleteEmi(emi)
        reminderScheduler.cancelReminder(emi.id)
    }

    /**
     * Records the next installment as paid. [penaltyAmount] is the late fine the user entered when
     * paying after the due date (0 when on time). Returns true if this was the final installment.
     */
    suspend fun markNextMonthPaid(emi: Emi, paidMonths: Int, penaltyAmount: Double = 0.0): Boolean {
        val nextMonth = paidMonths + 1
        val insertedId = emiDao.insertPayment(
            EmiPayment(
                emiId = emi.id,
                monthNumber = nextMonth,
                paidDateMillis = System.currentTimeMillis(),
                penaltyAmount = penaltyAmount,
            )
        )
        // -1 means this installment was already recorded (duplicate ignored) — don't re-run the
        // completion/reminder side effects off a payment that didn't actually happen.
        if (insertedId == -1L) return false
        val justCompleted = nextMonth >= emi.totalMonths
        val updatedEmi = if (justCompleted) {
            emi.copy(isCompleted = true).also { emiDao.updateEmi(it) }
        } else {
            emi
        }
        reminderScheduler.scheduleReminder(updatedEmi, nextMonth)
        return justCompleted
    }

    suspend fun undoLastPayment(emi: Emi) {
        val lastPayment = emiDao.getLastPayment(emi.id) ?: return
        emiDao.deletePayment(lastPayment)
        val updatedEmi = if (emi.isCompleted) {
            emi.copy(isCompleted = false).also { emiDao.updateEmi(it) }
        } else {
            emi
        }
        val paidMonths = emiDao.getPaidMonthCount(emi.id)
        reminderScheduler.scheduleReminder(updatedEmi, paidMonths)
    }
}
