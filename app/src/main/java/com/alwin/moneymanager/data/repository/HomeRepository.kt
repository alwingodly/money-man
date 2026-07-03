package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** An expense paired with its category's display name, for Home's activity preview. */
data class RecentExpenseItem(val expense: Expense, val categoryName: String)

data class HomeSummary(
    val todayTotal: Double,
    val monthExpenseOnly: Double,
    val monthIncludingEmi: Double,
    val monthlyAverageExpenseOnly: Double,
    val monthlyAverageIncludingEmi: Double,
    val emiDueThisMonth: Double,
    val totalEmiOutstanding: Double,
    val creditCardThisMonth: Double,
    val savingsThisMonth: Double,
)

private const val AVERAGE_WINDOW_MONTHS = 6L

@Singleton
class HomeRepository @Inject constructor(
    private val emiRepository: EmiRepository,
    private val expenseRepository: ExpenseRepository,
) {
    fun getHomeSummary(): Flow<HomeSummary> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val averageWindowStart = today.withDayOfMonth(1)
            .minusMonths(AVERAGE_WINDOW_MONTHS - 1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        // Nested first since the outer combine() overload tops out at 5 flows.
        val paymentMethodSplit = combine(
            expenseRepository.getExpenseTotalForPeriodByPaymentMethod(monthStart, monthEnd, true),
            expenseRepository.getExpenseTotalForPeriodByPaymentMethod(monthStart, monthEnd, false),
        ) { creditCard, savings -> creditCard to savings }

        return combine(
            emiRepository.getAllEmisWithProgress(),
            expenseRepository.getExpenseTotalForPeriod(todayStart, todayEnd),
            expenseRepository.getExpenseTotalForPeriod(monthStart, monthEnd),
            expenseRepository.getExpenseTotalForPeriod(averageWindowStart, monthEnd),
            paymentMethodSplit,
        ) { emis, todayTotal, monthExpense, windowExpense, (creditCardThisMonth, savingsThisMonth) ->
            // Counted by each installment's *due* month, not when it was actually marked paid —
            // see EmiRepository.paidAmountInRange for why (catching up on backlogged months in
            // one sitting shouldn't dump all of it into whichever month you happened to pay in).
            val monthEmiPaid = emis.paidAmountInRange(monthStart, monthEnd)
            val windowEmiPaid = emis.paidAmountInRange(averageWindowStart, monthEnd)
            val emiDueThisMonth = emis.dueAmountInRange(monthStart, monthEnd)
            val totalEmiOutstanding = emis.sumOf { it.remainingAmount }

            HomeSummary(
                todayTotal = todayTotal,
                monthExpenseOnly = monthExpense,
                monthIncludingEmi = monthExpense + monthEmiPaid,
                monthlyAverageExpenseOnly = windowExpense / AVERAGE_WINDOW_MONTHS,
                monthlyAverageIncludingEmi = (windowExpense + windowEmiPaid) / AVERAGE_WINDOW_MONTHS,
                emiDueThisMonth = emiDueThisMonth,
                totalEmiOutstanding = totalEmiOutstanding,
                creditCardThisMonth = creditCardThisMonth,
                savingsThisMonth = savingsThisMonth,
            )
        }
    }

    /** Active (not yet fully paid) EMIs, soonest-due first — used for Home's "Active loans" preview. */
    fun getActiveEmis(): Flow<List<EmiWithProgress>> =
        emiRepository.getAllEmisWithProgress().map { list ->
            list.filterNot { it.emi.isCompleted }
                .sortedBy { it.nextDueDateMillis ?: Long.MAX_VALUE }
        }

    fun getRecentActivity(limit: Int): Flow<List<RecentExpenseItem>> =
        combine(
            expenseRepository.getRecentExpenses(limit),
            expenseRepository.getAllCategories(),
        ) { expenses, categories ->
            val nameById = categories.associate { it.id to it.name }
            expenses.map { expense ->
                RecentExpenseItem(expense, nameById[expense.categoryId] ?: "Unknown")
            }
        }
}
