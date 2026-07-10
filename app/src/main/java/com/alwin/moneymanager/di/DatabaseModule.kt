package com.alwin.moneymanager.di

import android.content.Context
import androidx.room.Room
import com.alwin.moneymanager.data.local.MIGRATION_1_2
import com.alwin.moneymanager.data.local.MIGRATION_2_3
import com.alwin.moneymanager.data.local.MIGRATION_3_4
import com.alwin.moneymanager.data.local.MIGRATION_4_5
import com.alwin.moneymanager.data.local.MIGRATION_5_6
import com.alwin.moneymanager.data.local.MIGRATION_6_7
import com.alwin.moneymanager.data.local.MIGRATION_7_8
import com.alwin.moneymanager.data.local.MIGRATION_8_9
import com.alwin.moneymanager.data.local.MIGRATION_9_10
import com.alwin.moneymanager.data.local.MIGRATION_10_11
import com.alwin.moneymanager.data.local.MIGRATION_11_12
import com.alwin.moneymanager.data.local.MIGRATION_12_13
import com.alwin.moneymanager.data.local.MIGRATION_13_14
import com.alwin.moneymanager.data.local.MIGRATION_14_15
import com.alwin.moneymanager.data.local.MoneyManagerDatabase
import com.alwin.moneymanager.data.local.dao.DebtDao
import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.data.local.dao.SavingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyManagerDatabase =
        Room.databaseBuilder(
            context,
            MoneyManagerDatabase::class.java,
            MoneyManagerDatabase.DATABASE_NAME,
        )
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                MIGRATION_14_15,
            )
            .build()

    @Provides
    fun provideEmiDao(database: MoneyManagerDatabase): EmiDao = database.emiDao()

    @Provides
    fun provideExpenseDao(database: MoneyManagerDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideExpenseCategoryDao(database: MoneyManagerDatabase): ExpenseCategoryDao =
        database.expenseCategoryDao()

    @Provides
    fun provideDebtDao(database: MoneyManagerDatabase): DebtDao = database.debtDao()

    @Provides
    fun provideSavingDao(database: MoneyManagerDatabase): SavingDao = database.savingDao()
}
