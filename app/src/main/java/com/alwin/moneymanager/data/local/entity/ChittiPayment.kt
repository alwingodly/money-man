package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chitti_payment",
    foreignKeys = [
        ForeignKey(
            entity = Chitti::class,
            parentColumns = ["id"],
            childColumns = ["chittiId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chittiId")]
)
data class ChittiPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chittiId: Long,
    val monthNumber: Int,
    val amount: Double,
    val paidDateMillis: Long,
    val note: String = "",
)
