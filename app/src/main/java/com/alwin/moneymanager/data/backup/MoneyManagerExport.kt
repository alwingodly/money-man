package com.alwin.moneymanager.data.backup

import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.local.entity.ExpenseCategory
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
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
