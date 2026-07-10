package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One deposit into a [Saving]. Amounts can vary; the pot's total is the sum of these. */
@Serializable
@Entity(
    tableName = "saving_contribution",
    foreignKeys = [
        ForeignKey(
            entity = Saving::class,
            parentColumns = ["id"],
            childColumns = ["savingId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("savingId")],
)
data class SavingContribution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val savingId: Long,
    val amount: Double,
    val dateMillis: Long,
    val note: String = "",
)
