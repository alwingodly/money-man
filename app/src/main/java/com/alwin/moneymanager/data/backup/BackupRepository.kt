package com.alwin.moneymanager.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.alwin.moneymanager.data.local.MoneyManagerDatabase
import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.reminder.EmiReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val emiDao: EmiDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val reminderScheduler: EmiReminderScheduler,
    @ApplicationContext private val context: Context,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val export = MoneyManagerExport(
            exportedAtMillis = System.currentTimeMillis(),
            emis = emiDao.getAllEmisSnapshot(),
            emiPayments = emiDao.getAllPaymentsSnapshot(),
            expenseCategories = expenseCategoryDao.getAllCategoriesSnapshot(),
            expenses = expenseDao.getAllExpensesSnapshot(),
        )
        val content = json.encodeToString(export)
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray())
        } ?: throw IOException("Could not open the selected file for writing")
    }

    /**
     * Replaces every local row with the contents of [uri] inside one DB transaction — if
     * anything fails partway (bad JSON, I/O error), the whole thing rolls back and the existing
     * data is untouched. Old EMI reminders are cancelled before the wipe and the imported EMIs'
     * reminders are rescheduled after, since `AlarmManager` alarms don't get cleaned up by a
     * plain DB delete.
     */
    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw IOException("Could not open the selected file for reading")
        val export = json.decodeFromString<MoneyManagerExport>(content)

        val previousEmiIds = emiDao.getAllEmisSnapshot().map { it.id }

        database.withTransaction {
            emiDao.deleteAllEmis()
            expenseCategoryDao.deleteAllCategories()
            expenseCategoryDao.insertCategories(export.expenseCategories)
            emiDao.insertEmis(export.emis)
            expenseDao.insertExpenses(export.expenses)
            emiDao.insertPayments(export.emiPayments)
        }

        previousEmiIds.forEach { reminderScheduler.cancelReminder(it) }
        export.emis.filter { it.notificationEnabled && !it.isCompleted }.forEach { emi ->
            val paidMonths = emiDao.getPaidMonthCount(emi.id)
            reminderScheduler.scheduleReminder(emi, paidMonths)
        }
    }
}
