package com.alwin.moneymanager.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alwin.moneymanager.data.local.entity.Debt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** (Re)schedules a reminder on [Debt.dueDateMillis], or cancels it if the debt no longer needs one. */
    fun scheduleReminder(debt: Debt) {
        val dueDate = debt.dueDateMillis
        if (!debt.notificationEnabled || debt.isSettled || dueDate == null ||
            dueDate <= System.currentTimeMillis()
        ) {
            cancelReminder(debt.id)
            return
        }
        val pendingIntent = pendingIntentFor(debt.id, createIfNeeded = true)!!
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
    }

    fun cancelReminder(debtId: Long) {
        pendingIntentFor(debtId, createIfNeeded = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntentFor(debtId: Long, createIfNeeded: Boolean): PendingIntent? {
        val intent = Intent(context, DebtReminderReceiver::class.java).putExtra(EXTRA_DEBT_ID, debtId)
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (createIfNeeded) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, debtId.toInt(), intent, flags)
    }
}
