package com.alwin.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {

    @Query("SELECT * FROM expense_category ORDER BY name ASC")
    fun getAllCategories(): Flow<List<ExpenseCategory>>

    @Insert
    suspend fun insertCategory(category: ExpenseCategory): Long

    @Update
    suspend fun updateCategory(category: ExpenseCategory)

    @Query("SELECT * FROM expense_category")
    suspend fun getAllCategoriesSnapshot(): List<ExpenseCategory>

    @Insert
    suspend fun insertCategories(categories: List<ExpenseCategory>)

    /** Cascades to `expense` via the FK's `ON DELETE CASCADE`. */
    @Query("DELETE FROM expense_category")
    suspend fun deleteAllCategories()

    /** 
     * Deletes a category and all its expenses (CASCADE).
     * Returns the number of categories deleted (1 if successful, 0 if not found).
     */
    @Query("DELETE FROM expense_category WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long): Int

    /** Returns the number of expenses in a category. */
    @Query("SELECT COUNT(*) FROM expense WHERE categoryId = :categoryId")
    suspend fun getExpenseCount(categoryId: Long): Int
}
