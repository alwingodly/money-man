package com.alwin.moneymanager

import android.app.Application
import com.alwin.moneymanager.data.backup.AutoBackupObserver
import com.alwin.moneymanager.reminder.createDebtReminderNotificationChannel
import com.alwin.moneymanager.reminder.createEmiReminderNotificationChannel
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application() {

    @Inject lateinit var autoBackupObserver: AutoBackupObserver

    override fun onCreate() {
        super.onCreate()
        createEmiReminderNotificationChannel(this)
        createDebtReminderNotificationChannel(this)
        autoBackupObserver.register()
    }
}
