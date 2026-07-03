package com.alwin.moneymanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory

@Database(
    entities = [Emi::class, EmiPayment::class, Expense::class, ExpenseCategory::class],
    version = 7,
    exportSchema = true,
)
abstract class MoneyManagerDatabase : RoomDatabase() {
    abstract fun emiDao(): EmiDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao

    companion object {
        const val DATABASE_NAME = "moneymanager.db"
    }
}
