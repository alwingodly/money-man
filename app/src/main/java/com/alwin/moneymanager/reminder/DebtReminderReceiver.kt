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
import com.alwin.moneymanager.data.local.dao.DebtDao
import com.alwin.moneymanager.util.formatCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DebtReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var debtDao: DebtDao

    override fun onReceive(context: Context, intent: Intent) {
        val debtId = intent.getLongExtra(EXTRA_DEBT_ID, -1L)
        if (debtId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val debt = debtDao.getAllDebtsSnapshot().firstOrNull { it.id == debtId } ?: return@launch
                val net = debtDao.getBalance(debtId)
                if (debt.notificationEnabled && !debt.isSettled && net != 0.0) {
                    showNotification(context, debtId, debt.personName, isOwedToMe = net > 0, outstanding = kotlin.math.abs(net))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        debtId: Long,
        personName: String,
        isOwedToMe: Boolean,
        outstanding: Double,
    ) {
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
            debtId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (isOwedToMe) "Collect from $personName" else "Pay $personName"
        val body = if (isOwedToMe) {
            "${formatCurrency(outstanding)} is due to be collected today"
        } else {
            "${formatCurrency(outstanding)} is due to be paid today"
        }
        val notification = NotificationCompat.Builder(context, DEBT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(DEBT_NOTIFICATION_TAG, debtId.toInt(), notification)
    }
}
