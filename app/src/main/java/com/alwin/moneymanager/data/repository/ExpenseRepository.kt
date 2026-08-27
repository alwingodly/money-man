package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeleteCategoryResult {
    data class Success(val expenseCount: Int) : DeleteCategoryResult
    data object NotFound : DeleteCategoryResult
}

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
) {
    fun getAllCategories(): Flow<List<ExpenseCategory>> = expenseCategoryDao.getAllCategories()

    suspend fun addCategory(name: String, budgetLimit: Double? = null): Long =
        expenseCategoryDao.insertCategory(ExpenseCategory(name = name, budgetLimit = budgetLimit))

    suspend fun updateCategory(category: ExpenseCategory, name: String, budgetLimit: Double?) {
        expenseCategoryDao.updateCategory(category.copy(name = name, budgetLimit = budgetLimit))
    }

    /**
     * Deletes a category and all its expenses (CASCADE).
     * Returns a result indicating success or failure with expense count.
     */
    suspend fun deleteCategory(categoryId: Long): DeleteCategoryResult {
        val expenseCount = expenseCategoryDao.getExpenseCount(categoryId)
        val deleted = expenseCategoryDao.deleteCategory(categoryId)
        return if (deleted > 0) {
            DeleteCategoryResult.Success(expenseCount)
        } else {
            DeleteCategoryResult.NotFound
        }
    }

    fun getExpenseTotalForCategoryAndPeriod(categoryId: Long, startMillis: Long, endMillis: Long): Flow<Double> =
        expenseDao.getExpenseTotalForCategoryAndPeriod(categoryId, startMillis, endMillis)

    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(categoryId)

    fun getExpensesByCategoryAndPeriod(
        categoryId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<Expense>> = expenseDao.getExpensesByCategoryAndPeriod(categoryId, startMillis, endMillis)

    fun getExpenseTotalForPeriod(startMillis: Long, endMillis: Long): Flow<Double> =
        expenseDao.getExpenseTotalForPeriod(startMillis, endMillis)

    /** All expenses in the period, across every category — used for the "any day" date lookup. */
    fun getExpensesForPeriod(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForPeriod(startMillis, endMillis)

    fun getRecentExpenses(limit: Int): Flow<List<Expense>> = expenseDao.getRecentExpenses(limit)

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

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
