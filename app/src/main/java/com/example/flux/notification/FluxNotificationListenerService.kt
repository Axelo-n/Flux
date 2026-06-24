package com.example.flux.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.example.flux.MainActivity
import com.example.flux.R
import com.example.flux.data.TransactionDatabase
import com.example.flux.data.TransactionEntity
import com.example.flux.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class FluxNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: TransactionRepository

    override fun onCreate() {
        super.onCreate()
        val database = TransactionDatabase.getDatabase(applicationContext)
        repository = TransactionRepository(database.transactionDao(), database.parserRuleDao())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        startPersistentNotification()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, FluxNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        if (title.contains("Flux Recorded This")) return

        val allowedApps = listOf("com.bcadigital.blu", "com.example.flux")
        if (packageName !in allowedApps) return

        serviceScope.launch {
            val customRules = repository.getRulesSync()
            val transaction = NotificationTransactionParser.parse(title, text, customRules)

            if (transaction != null) {
                repository.insert(
                    TransactionEntity(
                        amount = transaction.amount,
                        note = transaction.note,
                        category = transaction.category,
                        isIncome = transaction.isIncome,
                        date = System.currentTimeMillis()
                    )
                )
                sendSuccessNotification(transaction)
            }
        }
    }

    private fun startPersistentNotification() {
        val channelId = "flux_persistent_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Flux Background Service", NotificationManager.IMPORTANCE_MIN)
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Flux Auto-Record is Active")
            .setContentText("Listening to bank notifications...")
            .setSmallIcon(R.drawable.flux_transparent)
            .setColor("#0B0E14".toColorInt())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1999, notification)
    }

    private fun sendSuccessNotification(tx: ParsedTransaction) {
        val channelId = "flux_success_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Flux Updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val amountString = format.format(tx.amount).replace("Rp", "Rp ")
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.flux_transparent)
            .setColor("#0B0E14".toColorInt())
            .setContentTitle("Flux Recorded This! ✅")
            .setContentText("$amountString (${tx.category})")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("FLUX_TRANSACTIONS")
            .build()

        val safeNotifId = (System.currentTimeMillis() % 100000).toInt() + 2000
        notificationManager.notify(safeNotifId, notification)
    }
}
