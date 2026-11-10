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

data class CategorySpend(val categoryId: Long, val name: String, val amount: Double)

data class RangeSpend(
    val categories: List<CategorySpend>,
    val extra: Double,
)

enum class SpendRange { MONTH, YEAR, ALL }

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
    val extraThisMonth: Double,
    val lastMonthToDate: Double,
    val debtToCollect: Double,
    val debtToPay: Double,
)

private const val AVERAGE_WINDOW_MONTHS = 6L

private data class ExtraTotals(
    val creditCard: Double,
    val savings: Double,
    val extra: Double,
    val lastMonthToDate: Double,
    val debt: DebtSummary,
)

@Singleton
class HomeRepository @Inject constructor(
    private val emiRepository: EmiRepository,
    private val expenseRepository: ExpenseRepository,
    private val debtRepository: DebtRepository,
) {
    fun getHomeSummary(): Flow<HomeSummary> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val lastMonthSameDay = today.minusMonths(1)
        val lastMonthStart = lastMonthSameDay.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val lastMonthSameDayEnd = lastMonthSameDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val averageWindowStart = today.withDayOfMonth(1)
            .minusMonths(AVERAGE_WINDOW_MONTHS - 1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        // Nested first since the outer combine() overload tops out at 5 flows — the payment-method
        // split and the debt summary are folded into one Triple here to stay within that limit.
        val extras = combine(
            expenseRepository.getExpenseTotalForPeriodByPaymentMethod(monthStart, monthEnd, true),
            expenseRepository.getExpenseTotalForPeriodByPaymentMethod(monthStart, monthEnd, false),
            expenseRepository.getExtraTotalForPeriod(monthStart, monthEnd),
            expenseRepository.getExpenseTotalForPeriod(lastMonthStart, lastMonthSameDayEnd),
            debtRepository.getSummary(),
        ) { creditCard, savings, extra, lastMonthToDate, debt ->
            ExtraTotals(creditCard, savings, extra, lastMonthToDate, debt)
        }

        return combine(
            emiRepository.getAllEmisWithProgress(),
            expenseRepository.getExpenseTotalForPeriod(todayStart, todayEnd),
            expenseRepository.getExpenseTotalForPeriod(monthStart, monthEnd),
            expenseRepository.getExpenseTotalForPeriod(averageWindowStart, monthEnd),
            extras,
        ) { emis, todayTotal, monthExpense, windowExpense, totals ->
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
                creditCardThisMonth = totals.creditCard,
                savingsThisMonth = totals.savings,
                extraThisMonth = totals.extra,
                lastMonthToDate = totals.lastMonthToDate,
                debtToCollect = totals.debt.totalToCollect,
                debtToPay = totals.debt.totalToPay,
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

    /** Spend split by category for the chosen range. No income or salary is required. */
    fun getCategorySpend(range: SpendRange): Flow<RangeSpend> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val expenses = when (range) {
            SpendRange.MONTH -> {
                val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
                expenseRepository.getExpensesForPeriod(start, end)
            }
            SpendRange.YEAR -> {
                val start = today.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.withDayOfYear(1).plusYears(1).atStartOfDay(zone).toInstant().toEpochMilli()
                expenseRepository.getExpensesForPeriod(start, end)
            }
            SpendRange.ALL -> expenseRepository.getAllExpenses()
        }
        return combine(expenses, expenseRepository.getAllCategories()) { list, categories ->
            val spent = list.groupBy { it.categoryId }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
            val knownIds = categories.map { it.id }.toSet()
            val known = categories.map { category ->
                CategorySpend(
                    categoryId = category.id,
                    name = category.name,
                    amount = spent[category.id] ?: 0.0,
                )
            }
            val unknown = spent
                .filterKeys { it !in knownIds }
                .map { (categoryId, amount) ->
                    CategorySpend(categoryId, "Unknown", amount)
                }
            (known + unknown).sortedByDescending { it.amount }.let { categories ->
                RangeSpend(
                    categories = categories,
                    extra = list.filter { it.isExtra }.sumOf { it.amount },
                )
            }
        }
    }
}
