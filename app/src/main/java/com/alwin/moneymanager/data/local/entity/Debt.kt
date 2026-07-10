package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A person's debt account — the digital paper `khata`, one account per person (no fixed direction).
 * All money you gave and all money you got back for this person live as [DebtEntry] rows, and the
 * **net** of them decides who currently owes whom (positive = they owe you, negative = you owe
 * them). This mirrors how Khatabook/OkCredit-style apps work: one running relationship per person.
 */
@Serializable
@Entity(tableName = "debt")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val note: String = "",
    val createdDateMillis: Long,
    /** Optional date the balance is expected to be cleared by; drives the reminder. */
    val dueDateMillis: Long? = null,
    val notificationEnabled: Boolean = false,
    /** Cached "net balance is zero / cleared" flag, refreshed whenever an entry changes. */
    val isSettled: Boolean = false,
)
