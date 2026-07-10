package com.alwin.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A savings pot. You add contributions of any amount over time (they can vary), and the running
 * total is the sum of them. [targetAmount] is an **optional** goal — set it to show progress toward
 * a number, or leave it null for an open-ended "just keep saving" pot.
 */
@Serializable
@Entity(tableName = "saving")
data class Saving(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Optional goal. Null = open-ended pot with no target. */
    val targetAmount: Double? = null,
    val note: String = "",
    val createdDateMillis: Long,
    /** Cached "reached the goal" flag, refreshed whenever a contribution changes. */
    val isAchieved: Boolean = false,
)
