package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "emi_payment",
    foreignKeys = [
        ForeignKey(
            entity = Emi::class,
            parentColumns = ["id"],
            childColumns = ["emiId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("emiId")],
)
data class EmiPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val emiId: Long,
    val monthNumber: Int,
    val paidDateMillis: Long,
)
