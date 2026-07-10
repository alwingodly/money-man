package com.alwin.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alwin.moneymanager.data.local.entity.Saving
import com.alwin.moneymanager.data.local.entity.SavingContribution
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingDao {

    @Query("SELECT * FROM saving ORDER BY isAchieved ASC, createdDateMillis DESC")
    fun getAllSavings(): Flow<List<Saving>>

    @Query("SELECT * FROM saving")
    suspend fun getAllSavingsSnapshot(): List<Saving>

    @Query("SELECT * FROM saving WHERE id = :savingId")
    fun getSavingById(savingId: Long): Flow<Saving?>

    @Insert
    suspend fun insertSaving(saving: Saving): Long

    @Update
    suspend fun updateSaving(saving: Saving)

    @Delete
    suspend fun deleteSaving(saving: Saving)

    @Query("SELECT * FROM saving_contribution WHERE savingId = :savingId ORDER BY dateMillis ASC, id ASC")
    fun getContributionsForSaving(savingId: Long): Flow<List<SavingContribution>>

    /** Every contribution across all pots, for the list/summary path — grouped in memory. */
    @Query("SELECT * FROM saving_contribution")
    fun getAllContributions(): Flow<List<SavingContribution>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM saving_contribution WHERE savingId = :savingId")
    suspend fun getSavedSum(savingId: Long): Double

    @Insert
    suspend fun insertContribution(contribution: SavingContribution): Long

    @Delete
    suspend fun deleteContribution(contribution: SavingContribution)

    @Query("SELECT * FROM saving_contribution")
    suspend fun getAllContributionsSnapshot(): List<SavingContribution>

    @Insert
    suspend fun insertSavings(savings: List<Saving>)

    @Insert
    suspend fun insertContributions(contributions: List<SavingContribution>)

    /** Cascades to `saving_contribution` via the FK's `ON DELETE CASCADE`. */
    @Query("DELETE FROM saving")
    suspend fun deleteAllSavings()
}
