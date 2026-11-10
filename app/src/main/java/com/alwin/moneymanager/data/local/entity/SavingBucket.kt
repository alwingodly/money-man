package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Something you want to buy or do, with how much it costs. Separate from [Saving] pots:
 * pots track money already put aside; a bucket list is the wishlist of amounts still needed.
 */
@Serializable
@Entity(tableName = "saving_bucket")
data class SavingBucket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val note: String = "",
    val isDone: Boolean = false,
    val createdDateMillis: Long,
)
