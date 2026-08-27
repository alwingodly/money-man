package com.alwin.moneymanager.data.repository

import com.alwin.moneymanager.data.local.dao.ChittiDao
import com.alwin.moneymanager.data.local.entity.Chitti
import com.alwin.moneymanager.data.local.entity.ChittiPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

data class ChittiWithProgress(
    val chitti: Chitti,
    val payments: List<ChittiPayment>,
) {
    val paidMonths: Int get() = payments.size
    val remainingMonths: Int get() = chitti.totalMonths - paidMonths
    val totalPaid: Double get() = payments.sumOf { it.amount }
    val totalReceived: Double get() = chitti.receivedAmount ?: 0.0
    val netPosition: Double get() = totalReceived - totalPaid
    val progressPercent: Float
        get() = if (chitti.totalMonths == 0) 0f else paidMonths.toFloat() / chitti.totalMonths
}

@Singleton
class ChittiRepository @Inject constructor(
    private val chittiDao: ChittiDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val allChittisWithProgress: Flow<List<ChittiWithProgress>> =
        combine(
            chittiDao.getAllChittis(),
            chittiDao.getAllPayments(),
        ) { chittis, payments ->
            val byId = payments.groupBy { it.chittiId }
            chittis.map { chitti ->
                ChittiWithProgress(chitti, byId[chitti.id].orEmpty())
            }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 1)

    fun getAllChittisWithProgress(): Flow<List<ChittiWithProgress>> = allChittisWithProgress

    fun getChittiWithProgress(chittiId: Long): Flow<ChittiWithProgress?> =
        combine(
            chittiDao.getChittiById(chittiId),
            chittiDao.getPaymentsForChitti(chittiId),
        ) { chitti, payments ->
            chitti?.let { ChittiWithProgress(it, payments) }
        }

    suspend fun insertChitti(chitti: Chitti): Long = chittiDao.insertChitti(chitti)

    suspend fun updateChitti(chitti: Chitti) = chittiDao.updateChitti(chitti)

    suspend fun deleteChitti(chitti: Chitti) = chittiDao.deleteChitti(chitti)

    suspend fun addPayment(payment: ChittiPayment): Long = chittiDao.insertPayment(payment)

    suspend fun updatePayment(payment: ChittiPayment) = chittiDao.updatePayment(payment)

    suspend fun deletePayment(payment: ChittiPayment) = chittiDao.deletePayment(payment)
}
