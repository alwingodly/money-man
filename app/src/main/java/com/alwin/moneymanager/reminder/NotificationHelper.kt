package com.alwin.moneymanager.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

const val NOTIFICATION_CHANNEL_ID = "emi_reminders"
const val EXTRA_EMI_ID = "extra_emi_id"

fun createEmiReminderNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        "EMI Reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Reminders for upcoming EMI due dates"
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}
