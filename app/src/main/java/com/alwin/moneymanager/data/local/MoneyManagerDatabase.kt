package com.alwin.moneymanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alwin.moneymanager.data.local.dao.DebtDao
import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.data.local.dao.SavingDao
import com.alwin.moneymanager.data.local.entity.Debt
import com.alwin.moneymanager.data.local.entity.DebtEntry
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.data.local.entity.Saving
import com.alwin.moneymanager.data.local.entity.SavingContribution

@Database(
    entities = [
        Emi::class, EmiPayment::class, Expense::class, ExpenseCategory::class,
        Debt::class, DebtEntry::class,
        Saving::class, SavingContribution::class,
    ],
    version = 14,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MoneyManagerDatabase : RoomDatabase() {
    abstract fun emiDao(): EmiDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun debtDao(): DebtDao
    abstract fun savingDao(): SavingDao

    companion object {
        const val DATABASE_NAME = "moneymanager.db"
    }
}
