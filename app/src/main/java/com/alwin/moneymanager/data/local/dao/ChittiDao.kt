package com.alwin.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alwin.moneymanager.data.local.entity.Chitti
import com.alwin.moneymanager.data.local.entity.ChittiPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface ChittiDao {

    @Query("SELECT * FROM chitti ORDER BY isCompleted ASC, startDateMillis DESC")
    fun getAllChittis(): Flow<List<Chitti>>

    @Query("SELECT * FROM chitti")
    suspend fun getAllChittisSnapshot(): List<Chitti>

    @Query("SELECT * FROM chitti WHERE id = :chittiId")
    fun getChittiById(chittiId: Long): Flow<Chitti?>

    @Insert
    suspend fun insertChitti(chitti: Chitti): Long

    @Update
    suspend fun updateChitti(chitti: Chitti)

    @Delete
    suspend fun deleteChitti(chitti: Chitti)

    @Query("SELECT * FROM chitti_payment WHERE chittiId = :chittiId ORDER BY monthNumber ASC")
    fun getPaymentsForChitti(chittiId: Long): Flow<List<ChittiPayment>>

    @Query("SELECT COUNT(*) FROM chitti_payment WHERE chittiId = :chittiId")
    suspend fun getPaidMonthCount(chittiId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPayment(payment: ChittiPayment): Long

    @Update
    suspend fun updatePayment(payment: ChittiPayment)

    @Query("SELECT * FROM chitti_payment WHERE chittiId = :chittiId ORDER BY monthNumber DESC LIMIT 1")
    suspend fun getLastPayment(chittiId: Long): ChittiPayment?

    @Delete
    suspend fun deletePayment(payment: ChittiPayment)

    @Query("SELECT * FROM chitti_payment ORDER BY chittiId, monthNumber")
    fun getAllPayments(): Flow<List<ChittiPayment>>

    @Query("SELECT * FROM chitti_payment")
    suspend fun getAllPaymentsSnapshot(): List<ChittiPayment>

    @Insert
    suspend fun insertChittis(chittis: List<Chitti>)

    @Insert
    suspend fun insertPayments(payments: List<ChittiPayment>)

    @Query("DELETE FROM chitti")
    suspend fun deleteAllChittis()
}
