package com.alwin.moneymanager

import android.app.Application
import com.alwin.moneymanager.reminder.createDebtReminderNotificationChannel
import com.alwin.moneymanager.reminder.createEmiReminderNotificationChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoneyManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createEmiReminderNotificationChannel(this)
        createDebtReminderNotificationChannel(this)
    }
}
