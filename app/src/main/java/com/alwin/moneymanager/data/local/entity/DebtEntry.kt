package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * One line in a [Debt] account's history. [isGiven] true = **you gave** money to the person (pushes
 * the balance toward them owing you, +), false = **you got** money from them (pushes it toward you
 * owing them, −). The account's net balance is Σgiven − Σgot; its sign decides the direction.
 */
@Serializable
@Entity(
    tableName = "debt_entry",
    foreignKeys = [
        ForeignKey(
            entity = Debt::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("debtId")],
)
data class DebtEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val isGiven: Boolean,
    val amount: Double,
    val dateMillis: Long,
    val note: String = "",
)
