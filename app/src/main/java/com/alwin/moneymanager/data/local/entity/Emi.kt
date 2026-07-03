package com.alwin.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "emi")
data class Emi(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val monthlyAmount: Double,
    val totalMonths: Int,
    val startDateMillis: Long,
    @ColumnInfo(defaultValue = "0")
    val endDateMillis: Long = 0,
    val notes: String = "",
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val notificationEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "3")
    val reminderDaysBefore: Int = 3,
    /** Original principal borrowed. 0 = unknown/not entered (e.g. pre-existing EMIs from before
     * this field existed) — interest calculations are hidden in that case rather than shown as 0. */
    @ColumnInfo(defaultValue = "0")
    val loanAmount: Double = 0.0,
)
