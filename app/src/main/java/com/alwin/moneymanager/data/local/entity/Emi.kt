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
    /** Per-installment amount. Named for the original monthly-only model; for weekly/daily EMIs
     * (see [frequency]) it's the weekly/daily amount. */
    val monthlyAmount: Double,
    /** Total number of installments. For weekly/daily EMIs this is the total weeks/days. */
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
    /** How often an installment falls due. Existing rows default to MONTHLY (unchanged behaviour). */
    @ColumnInfo(defaultValue = "MONTHLY")
    val frequency: EmiFrequency = EmiFrequency.MONTHLY,
    /** Off-day weekdays for a DAILY EMI, as a bitmask over java.time DayOfWeek values (bit n set =
     * that weekday is skipped, Monday=1 … Sunday=7). 0 = every day is a working day. Ignored for
     * non-daily frequencies. */
    @ColumnInfo(defaultValue = "0")
    val offDaysMask: Int = 0,
    /** Days between installments for an [EmiFrequency.CUSTOM] EMI (e.g. 28 = every 28 days).
     * Ignored for other frequencies. */
    @ColumnInfo(defaultValue = "0")
    val intervalDays: Int = 0,
)
