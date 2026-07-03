package com.alwin.moneymanager.data.local.entity

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
)
