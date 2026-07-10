package com.alwin.moneymanager.data.backup

import com.alwin.moneymanager.data.local.entity.Debt
import com.alwin.moneymanager.data.local.entity.DebtEntry
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
import com.alwin.moneymanager.data.local.entity.Saving
import com.alwin.moneymanager.data.local.entity.SavingContribution
import kotlinx.serialization.Serializable

/**
 * Full snapshot of every user-editable table, written as one JSON file for local backup/restore
 * (and, later, WhatsApp export — same format, different destination `Intent`). Row `id`s are
 * preserved on export/import so foreign keys (`EmiPayment.emiId`, `Expense.categoryId`) still
 * resolve correctly after a restore.
 */
@Serializable
data class MoneyManagerExport(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long,
    val emis: List<Emi>,
    val emiPayments: List<EmiPayment>,
    val expenseCategories: List<ExpenseCategory>,
    val expenses: List<Expense>,
    // Added after the initial format. Default to empty so backups written by older app versions
    // (which have no debt keys) still decode and restore cleanly.
    val debts: List<Debt> = emptyList(),
    val debtEntries: List<DebtEntry> = emptyList(),
    val savings: List<Saving> = emptyList(),
    val savingContributions: List<SavingContribution> = emptyList(),
) {
    companion object {
        // Intentionally left at 1 even though debt fields were added: the new fields have safe
        // defaults, so an older app can still restore the parts it understands rather than
        // rejecting the whole file. Bump only for a breaking format change.
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
