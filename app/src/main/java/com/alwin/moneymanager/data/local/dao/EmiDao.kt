package com.alwin.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface EmiDao {

    @Query("SELECT * FROM emi ORDER BY isCompleted ASC, startDateMillis DESC")
    fun getAllEmis(): Flow<List<Emi>>

    @Query("SELECT * FROM emi")
    suspend fun getAllEmisSnapshot(): List<Emi>

    @Query("SELECT * FROM emi WHERE id = :emiId")
    fun getEmiById(emiId: Long): Flow<Emi?>

    @Insert
    suspend fun insertEmi(emi: Emi): Long

    @Update
    suspend fun updateEmi(emi: Emi)

    @Delete
    suspend fun deleteEmi(emi: Emi)

    @Query("SELECT * FROM emi_payment WHERE emiId = :emiId ORDER BY monthNumber ASC")
    fun getPaymentsForEmi(emiId: Long): Flow<List<EmiPayment>>

    @Query("SELECT COUNT(*) FROM emi_payment WHERE emiId = :emiId")
    suspend fun getPaidMonthCount(emiId: Long): Int

    /**
     * IGNORE so a duplicate (emiId, monthNumber) — e.g. from a rapid double-tap on "mark paid"
     * where the second call still sees stale paidMonths — silently no-ops against the unique
     * index instead of crashing. Returns the new rowId, or -1 when the insert was ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPayment(payment: EmiPayment): Long

    @Query("SELECT * FROM emi_payment WHERE emiId = :emiId ORDER BY monthNumber DESC LIMIT 1")
    suspend fun getLastPayment(emiId: Long): EmiPayment?

    @Delete
    suspend fun deletePayment(payment: EmiPayment)

    @Query("SELECT * FROM emi_payment")
    suspend fun getAllPaymentsSnapshot(): List<EmiPayment>

    @Insert
    suspend fun insertEmis(emis: List<Emi>)

    @Insert
    suspend fun insertPayments(payments: List<EmiPayment>)

    /** Cascades to `emi_payment` via the FK's `ON DELETE CASCADE`. */
    @Query("DELETE FROM emi")
    suspend fun deleteAllEmis()
}
