package com.alwin.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "expense_category", indices = [Index(value = ["name"], unique = true)])
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Optional monthly spending limit for this category. Null = no budget set. */
    val budgetLimit: Double? = null,
    /** Chip order on the Expenses screen. Lower comes first. */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
)
