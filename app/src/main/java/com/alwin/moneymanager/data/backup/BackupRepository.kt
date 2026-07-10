package com.alwin.moneymanager.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.alwin.moneymanager.data.local.MoneyManagerDatabase
import com.alwin.moneymanager.data.local.dao.DebtDao
import com.alwin.moneymanager.data.local.dao.EmiDao
import com.alwin.moneymanager.data.local.dao.ExpenseCategoryDao
import com.alwin.moneymanager.data.local.dao.ExpenseDao
import com.alwin.moneymanager.data.local.dao.SavingDao
import com.alwin.moneymanager.data.repository.BackupReminderRepository
import com.alwin.moneymanager.reminder.DebtReminderScheduler
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
    private val debtDao: DebtDao,
    private val savingDao: SavingDao,
    private val reminderScheduler: EmiReminderScheduler,
    private val debtReminderScheduler: DebtReminderScheduler,
    private val backupReminderRepository: BackupReminderRepository,
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
            debts = debtDao.getAllDebtsSnapshot(),
            debtEntries = debtDao.getAllEntriesSnapshot(),
            savings = savingDao.getAllSavingsSnapshot(),
            savingContributions = savingDao.getAllContributionsSnapshot(),
        )
        val content = json.encodeToString(export)
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray())
        } ?: throw IOException("Could not open the selected file for writing")
        backupReminderRepository.recordBackup()
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
        if (export.schemaVersion > MoneyManagerExport.CURRENT_SCHEMA_VERSION) {
            throw IOException("This backup was created with a newer version of the app. Update the app before restoring it.")
        }

        val previousEmiIds = emiDao.getAllEmisSnapshot().map { it.id }
        val previousDebtIds = debtDao.getAllDebtsSnapshot().map { it.id }

        database.withTransaction {
            emiDao.deleteAllEmis()
            expenseCategoryDao.deleteAllCategories()
            debtDao.deleteAllDebts()
            savingDao.deleteAllSavings()
            expenseCategoryDao.insertCategories(export.expenseCategories)
            emiDao.insertEmis(export.emis)
            expenseDao.insertExpenses(export.expenses)
            emiDao.insertPayments(export.emiPayments)
            debtDao.insertDebts(export.debts)
            debtDao.insertEntries(export.debtEntries)
            savingDao.insertSavings(export.savings)
            savingDao.insertContributions(export.savingContributions)
            resyncAutoIncrementCounters()
        }

        previousEmiIds.forEach { reminderScheduler.cancelReminder(it) }
        export.emis.filter { it.notificationEnabled && !it.isCompleted }.forEach { emi ->
            val paidMonths = emiDao.getPaidMonthCount(emi.id)
            reminderScheduler.scheduleReminder(emi, paidMonths)
        }

        previousDebtIds.forEach { debtReminderScheduler.cancelReminder(it) }
        export.debts.filter { it.notificationEnabled && !it.isSettled }.forEach { debt ->
            debtReminderScheduler.scheduleReminder(debt)
        }
    }

    /**
     * Restoring re-inserts rows with their original ids (via plain `@Insert`, not autogenerated)
     * so foreign keys keep resolving, but that bypasses SQLite's `sqlite_sequence` counter.
     * Without this, a later plain insert of a brand-new row can collide with a restored row's id
     * once the counter falls behind the highest id actually present in the table.
     */
    private fun resyncAutoIncrementCounters() {
        val db = database.openHelper.writableDatabase
        listOf(
            "emi", "emi_payment", "expense", "expense_category",
            "debt", "debt_entry", "saving", "saving_contribution",
        ).forEach { table ->
            db.execSQL(
                "INSERT OR REPLACE INTO sqlite_sequence(name, seq) " +
                    "VALUES('$table', (SELECT COALESCE(MAX(id), 0) FROM $table))"
            )
        }
    }
}
