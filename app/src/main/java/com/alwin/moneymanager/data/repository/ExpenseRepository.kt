package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
) {
    fun getAllCategories(): Flow<List<ExpenseCategory>> = expenseCategoryDao.getAllCategories()

    suspend fun addCategory(name: String): Long =
        expenseCategoryDao.insertCategory(ExpenseCategory(name = name))

    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(categoryId)

    fun getExpenseTotalForPeriod(startMillis: Long, endMillis: Long): Flow<Double> =
        expenseDao.getExpenseTotalForPeriod(startMillis, endMillis)

    /** All expenses in the period, across every category — used for the "any day" date lookup. */
    fun getExpensesForPeriod(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForPeriod(startMillis, endMillis)

    fun getRecentExpenses(limit: Int): Flow<List<Expense>> = expenseDao.getRecentExpenses(limit)

    fun searchExpenses(query: String): Flow<List<Expense>> = expenseDao.searchExpenses(query.trim())

    fun getExpenseTotalForPeriodByPaymentMethod(
        startMillis: Long,
        endMillis: Long,
        isCreditCard: Boolean,
    ): Flow<Double> = expenseDao.getExpenseTotalForPeriodByPaymentMethod(startMillis, endMillis, isCreditCard)

    suspend fun addExpense(
        categoryId: Long,
        amount: Double,
        note: String,
        dateMillis: Long,
        isCreditCard: Boolean,
    ) {
        expenseDao.insertExpense(
            Expense(
                categoryId = categoryId,
                amount = amount,
                note = note,
                dateMillis = dateMillis,
                isCreditCard = isCreditCard,
            )
        )
    }

    suspend fun updateExpense(
        expense: Expense,
        categoryId: Long,
        amount: Double,
        note: String,
        dateMillis: Long,
        isCreditCard: Boolean,
    ) {
        expenseDao.updateExpense(
            expense.copy(
                categoryId = categoryId,
                amount = amount,
                note = note,
                dateMillis = dateMillis,
                isCreditCard = isCreditCard,
            )
        )
    }

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    /** Re-inserts a just-deleted expense with its original id preserved (for Snackbar undo). */
    suspend fun restoreExpense(expense: Expense) = expenseDao.insertExpense(expense)
}
