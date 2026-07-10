package com.alwin.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alwin.moneymanager.data.local.entity.Debt
import com.alwin.moneymanager.data.local.entity.DebtEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debt ORDER BY isSettled ASC, createdDateMillis DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debt")
    suspend fun getAllDebtsSnapshot(): List<Debt>

    @Query("SELECT * FROM debt WHERE id = :debtId")
    fun getDebtById(debtId: Long): Flow<Debt?>

    /** The account for a person, if one already exists — used to append to it instead of creating a
     * duplicate (one account per person). Case-insensitive so "Ammu"/"ammu" resolve to one. */
    @Query("SELECT * FROM debt WHERE personName = :personName COLLATE NOCASE LIMIT 1")
    suspend fun findAccount(personName: String): Debt?

    @Insert
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("SELECT * FROM debt_entry WHERE debtId = :debtId ORDER BY dateMillis ASC, id ASC")
    fun getEntriesForDebt(debtId: Long): Flow<List<DebtEntry>>

    /** Every entry across all accounts, for the list/summary path — one cursor grouped in memory
     * instead of a per-account query each (see DebtRepository.getAllDebtsWithProgress). */
    @Query("SELECT * FROM debt_entry")
    fun getAllEntries(): Flow<List<DebtEntry>>

    /** Signed net balance: (money you gave) − (money you got). Positive = they owe you. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN isGiven THEN amount ELSE -amount END), 0) " +
            "FROM debt_entry WHERE debtId = :debtId"
    )
    suspend fun getBalance(debtId: Long): Double

    @Insert
    suspend fun insertEntry(entry: DebtEntry): Long

    @Delete
    suspend fun deleteEntry(entry: DebtEntry)

    @Query("SELECT * FROM debt_entry")
    suspend fun getAllEntriesSnapshot(): List<DebtEntry>

    @Insert
    suspend fun insertDebts(debts: List<Debt>)

    @Insert
    suspend fun insertEntries(entries: List<DebtEntry>)

    /** Cascades to `debt_entry` via the FK's `ON DELETE CASCADE`. */
    @Query("DELETE FROM debt")
    suspend fun deleteAllDebts()
}
