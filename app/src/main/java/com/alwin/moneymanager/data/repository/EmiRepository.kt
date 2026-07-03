package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.reminder.EmiReminderScheduler
import com.alwin.moneymanager.util.addMonths
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
        get() = if (paidMonths < emi.totalMonths) addMonths(emi.startDateMillis, paidMonths) else null

    val totalPayable: Double get() = emi.monthlyAmount * emi.totalMonths

    /** Total interest over the life of the loan, or null when [Emi.loanAmount] hasn't been
     * entered (0 = unknown, not a real zero-interest loan) — see [Emi.loanAmount]. */
    val totalInterest: Double?
        get() = if (emi.loanAmount > 0) totalPayable - emi.loanAmount else null
}

data class EmiMonthSummary(val paidAmount: Double, val dueAmount: Double)

/**
 * Sum of `monthlyAmount` for every installment whose *due date* (not paid date) falls in
 * [startMillis, endMillis) and has already been paid. Due-date based so catching up on
 * backlogged months in one sitting doesn't inflate whichever month you happened to pay in —
 * see `HomeRepository` for the bug this pattern was fixed to avoid.
 */
fun List<EmiWithProgress>.paidAmountInRange(startMillis: Long, endMillis: Long): Double =
    sumOf { emiWithProgress ->
        val count = emiWithProgress.payments.count { payment ->
            val dueDateMillis = addMonths(emiWithProgress.emi.startDateMillis, payment.monthNumber - 1)
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
    fun getAllEmisWithProgress(): Flow<List<EmiWithProgress>> =
        emiDao.getAllEmis().flatMapLatest { emis ->
            if (emis.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(emis.map { emi -> emiWithProgressFlow(emi) }) { it.toList() }
            }
        }

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
        endDateMillis: Long,
        notes: String,
        notificationEnabled: Boolean,
        reminderDaysBefore: Int,
        loanAmount: Double,
    ) {
        val emi = Emi(
            name = name,
            monthlyAmount = monthlyAmount,
            totalMonths = totalMonths,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            notes = notes,
            notificationEnabled = notificationEnabled,
            reminderDaysBefore = reminderDaysBefore,
            loanAmount = loanAmount,
        )
        val id = emiDao.insertEmi(emi)
        reminderScheduler.scheduleReminder(emi.copy(id = id), paidMonths = 0)
    }

    suspend fun updateEmiDetails(
        emi: Emi,
        name: String,
        monthlyAmount: Double,
        totalMonths: Int,
        startDateMillis: Long,
        endDateMillis: Long,
        notes: String,
        notificationEnabled: Boolean,
        reminderDaysBefore: Int,
        loanAmount: Double,
    ) {
        val updated = emi.copy(
            name = name,
            monthlyAmount = monthlyAmount,
            totalMonths = totalMonths,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            notes = notes,
            notificationEnabled = notificationEnabled,
            reminderDaysBefore = reminderDaysBefore,
            loanAmount = loanAmount,
        )
        emiDao.updateEmi(updated)
        val paidMonths = emiDao.getPaidMonthCount(updated.id)
        reminderScheduler.scheduleReminder(updated, paidMonths)
    }

    suspend fun deleteEmi(emi: Emi) {
        emiDao.deleteEmi(emi)
        reminderScheduler.cancelReminder(emi.id)
    }

    /** Returns true if this payment was the final installment, completing the loan. */
    suspend fun markNextMonthPaid(emi: Emi, paidMonths: Int): Boolean {
        val nextMonth = paidMonths + 1
        emiDao.insertPayment(
            EmiPayment(
                emiId = emi.id,
                monthNumber = nextMonth,
                paidDateMillis = System.currentTimeMillis(),
            )
        )
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
