package com.alwin.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chitti")
data class Chitti(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val totalMonths: Int,
    val startDateMillis: Long,
    val notes: String = "",
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "MONTHLY")
    val frequency: ChittiFrequency = ChittiFrequency.MONTHLY,
    @ColumnInfo(defaultValue = "NULL")
    val receivedAmount: Double? = null,
    @ColumnInfo(defaultValue = "NULL")
    val receivedMonthNumber: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val receivedDateMillis: Long? = null,
)
