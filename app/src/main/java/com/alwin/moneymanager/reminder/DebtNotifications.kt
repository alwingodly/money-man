package com.alwin.moneymanager.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

const val DEBT_NOTIFICATION_CHANNEL_ID = "debt_reminders"
const val EXTRA_DEBT_ID = "extra_debt_id"

/** Tag used when posting debt notifications so their ids never collide with EMI notification ids. */
const val DEBT_NOTIFICATION_TAG = "debt"

fun createDebtReminderNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        DEBT_NOTIFICATION_CHANNEL_ID,
        "Debt Reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Reminders for money you'll get back or need to give on a due date"
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}
