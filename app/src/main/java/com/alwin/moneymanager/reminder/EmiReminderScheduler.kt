package com.alwin.moneymanager.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.util.addMonths
import com.alwin.moneymanager.util.subtractDays
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmiReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** (Re)schedules the reminder for [emi]'s next unpaid month, or cancels it if not applicable. */
    fun scheduleReminder(emi: Emi, paidMonths: Int) {
        if (!emi.notificationEnabled || emi.isCompleted || paidMonths >= emi.totalMonths) {
            cancelReminder(emi.id)
            return
        }
        val dueDateMillis = addMonths(emi.startDateMillis, paidMonths)
        val triggerAtMillis = subtractDays(dueDateMillis, emi.reminderDaysBefore)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            cancelReminder(emi.id)
            return
        }
        val pendingIntent = pendingIntentFor(emi.id, createIfNeeded = true)!!
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun cancelReminder(emiId: Long) {
        pendingIntentFor(emiId, createIfNeeded = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntentFor(emiId: Long, createIfNeeded: Boolean): PendingIntent? {
        val intent = Intent(context, EmiReminderReceiver::class.java).putExtra(EXTRA_EMI_ID, emiId)
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (createIfNeeded) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, emiId.toInt(), intent, flags)
    }
}
