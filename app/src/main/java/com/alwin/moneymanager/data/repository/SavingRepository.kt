package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.SavingDao
import com.alwin.moneymanager.data.local.entity.Saving
import com.alwin.moneymanager.data.local.entity.SavingContribution
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** A savings pot plus its contributions, with the running total and optional goal progress. */
data class SavingWithProgress(
    val saving: Saving,
    val contributions: List<SavingContribution>,
) {
    val totalSaved: Double get() = contributions.sumOf { it.amount }

    /** Progress toward the goal (0..1), or null when there's no target. */
    val progress: Float?
        get() {
            val target = saving.targetAmount ?: return null
            if (target <= 0) return null
            return (totalSaved / target).toFloat().coerceIn(0f, 1f)
        }

    /** Amount still needed to reach the goal, or null when there's no target. */
    val remaining: Double?
        get() = saving.targetAmount?.let { (it - totalSaved).coerceAtLeast(0.0) }

    val isAchieved: Boolean
        get() = saving.targetAmount?.let { totalSaved >= it } ?: false
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SavingRepository @Inject constructor(
    private val savingDao: SavingDao,
) {
    // One cursor for pots + one for all contributions, joined in memory (same pattern as debts).
    fun getAllSavingsWithProgress(): Flow<List<SavingWithProgress>> =
        combine(savingDao.getAllSavings(), savingDao.getAllContributions()) { savings, contributions ->
            val byId = contributions.groupBy { it.savingId }
            savings.map { saving -> SavingWithProgress(saving, byId[saving.id].orEmpty()) }
        }

    fun getSavingWithProgress(savingId: Long): Flow<SavingWithProgress?> =
        savingDao.getSavingById(savingId).flatMapLatest { saving ->
            if (saving == null) flowOf(null) else savingContributionsFlow(saving)
        }

    /** Total saved across every pot, for the list header. */
    fun getTotalSaved(): Flow<Double> = getAllSavingsWithProgress().map { list -> list.sumOf { it.totalSaved } }

    private fun savingContributionsFlow(saving: Saving): Flow<SavingWithProgress> =
        savingDao.getContributionsForSaving(saving.id).map { c -> SavingWithProgress(saving, c) }

    suspend fun addSaving(name: String, targetAmount: Double?, note: String) {
        savingDao.insertSaving(
            Saving(
                name = name,
                targetAmount = targetAmount,
                note = note,
                createdDateMillis = System.currentTimeMillis(),
                isAchieved = false,
            ),
        )
    }

    suspend fun updateSaving(saving: Saving, name: String, targetAmount: Double?, note: String) {
        val paidSum = savingDao.getSavedSum(saving.id)
        savingDao.updateSaving(
            saving.copy(
                name = name,
                targetAmount = targetAmount,
                note = note,
                isAchieved = targetAmount != null && paidSum >= targetAmount,
            ),
        )
    }

    suspend fun deleteSaving(saving: Saving) = savingDao.deleteSaving(saving)

    suspend fun addContribution(saving: Saving, amount: Double, dateMillis: Long) {
        savingDao.insertContribution(
            SavingContribution(savingId = saving.id, amount = amount, dateMillis = dateMillis),
        )
        refreshAchieved(saving)
    }

    suspend fun deleteContribution(saving: Saving, contribution: SavingContribution) {
        savingDao.deleteContribution(contribution)
        refreshAchieved(saving)
    }

    suspend fun restoreContribution(saving: Saving, contribution: SavingContribution) {
        savingDao.insertContribution(contribution.copy(id = 0))
        refreshAchieved(saving)
    }

    private suspend fun refreshAchieved(saving: Saving) {
        val target = saving.targetAmount ?: return
        val achieved = savingDao.getSavedSum(saving.id) >= target
        if (achieved != saving.isAchieved) {
            savingDao.updateSaving(saving.copy(isAchieved = achieved))
        }
    }
}
