package com.alwin.moneymanager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alwin.moneymanager.data.local.dao.DebtDao
import com.alwin.moneymanager.data.local.dao.EmiDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var emiDao: EmiDao

    @Inject
    lateinit var scheduler: EmiReminderScheduler

    @Inject
    lateinit var debtDao: DebtDao

    @Inject
    lateinit var debtScheduler: DebtReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                emiDao.getAllEmisSnapshot()
                    .filter { it.notificationEnabled && !it.isCompleted }
                    .forEach { emi ->
                        val paidMonths = emiDao.getPaidMonthCount(emi.id)
                        scheduler.scheduleReminder(emi, paidMonths)
                    }
                debtDao.getAllDebtsSnapshot()
                    .filter { it.notificationEnabled && !it.isSettled }
                    .forEach { debt -> debtScheduler.scheduleReminder(debt) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
