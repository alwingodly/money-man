package com.alwin.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alwin.moneymanager.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expense WHERE categoryId = :categoryId ORDER BY dateMillis DESC")
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense WHERE dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getExpenseTotalForPeriod(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM expense " +
            "WHERE categoryId = :categoryId AND dateMillis >= :startMillis AND dateMillis < :endMillis"
    )
    fun getExpenseTotalForCategoryAndPeriod(categoryId: Long, startMillis: Long, endMillis: Long): Flow<Double>

    @Query("SELECT * FROM expense WHERE dateMillis >= :startMillis AND dateMillis < :endMillis ORDER BY dateMillis DESC")
    fun getExpensesForPeriod(startMillis: Long, endMillis: Long): Flow<List<Expense>>

    /** Most recently logged expenses, across every category — used for Home's activity preview. */
    @Query("SELECT * FROM expense ORDER BY dateMillis DESC, id DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int): Flow<List<Expense>>

    /** Every expense, newest first — used for the month/year summary breakdown. */
    @Query("SELECT * FROM expense ORDER BY dateMillis DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Free-text search across a note or its category name, plus an exact amount match so a user
     * can find "that ₹500 thing." [query] is matched case-insensitively as a substring; callers
     * pass a non-blank, trimmed string. Newest first.
     */
    @Query(
        "SELECT expense.* FROM expense " +
            "LEFT JOIN expense_category ON expense.categoryId = expense_category.id " +
            "WHERE expense.note LIKE '%' || :query || '%' " +
            "OR expense_category.name LIKE '%' || :query || '%' " +
            "OR CAST(expense.amount AS TEXT) LIKE :query || '%' " +
            "ORDER BY expense.dateMillis DESC, expense.id DESC"
    )
    fun searchExpenses(query: String): Flow<List<Expense>>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM expense " +
            "WHERE dateMillis >= :startMillis AND dateMillis < :endMillis AND isCreditCard = :isCreditCard"
    )
    fun getExpenseTotalForPeriodByPaymentMethod(
        startMillis: Long,
        endMillis: Long,
        isCreditCard: Boolean,
    ): Flow<Double>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expense")
    suspend fun getAllExpensesSnapshot(): List<Expense>

    @Insert
    suspend fun insertExpenses(expenses: List<Expense>)
}
