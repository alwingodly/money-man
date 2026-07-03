package com.alwin.moneymanager.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alwin.moneymanager.MainActivity
import com.alwin.moneymanager.R
import com.alwin.moneymanager.data.local.dao.EmiDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmiReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var emiDao: EmiDao

    override fun onReceive(context: Context, intent: Intent) {
        val emiId = intent.getLongExtra(EXTRA_EMI_ID, -1L)
        if (emiId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emi = emiDao.getAllEmisSnapshot().firstOrNull { it.id == emiId } ?: return@launch
                val paidMonths = emiDao.getPaidMonthCount(emiId)
                if (emi.notificationEnabled && !emi.isCompleted && paidMonths < emi.totalMonths) {
                    showNotification(context, emiId, emi.name, emi.reminderDaysBefore)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, emiId: Long, name: String, reminderDaysBefore: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            emiId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val daysText = if (reminderDaysBefore == 0) "today" else "in $reminderDaysBefore day(s)"
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("EMI due soon: $name")
            .setContentText("Payment is due $daysText")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(emiId.toInt(), notification)
    }
}
