package com.example.flux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.core.graphics.toColorInt

class FluxNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: TransactionRepository

    override fun onCreate() {
        super.onCreate()
        val database = TransactionDatabase.getDatabase(applicationContext)
        repository = TransactionRepository(database.transactionDao(), parserRuleDao = database.parserRuleDao())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        // 1. INITIALIZE FILTER
        val packageName = sbn.packageName

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        // Prevent loop dari debug
        if (title.contains("Flux Recorded This")) return

        // --- FILTERING ---
        val allowedApps = listOf("com.bcadigital.blu", "com.example.flux")
        if (packageName !in allowedApps) return

        // 3. PARSING
        serviceScope.launch {
            // A. Initialize Rules
            val customRules = repository.getRulesSync()

            // B. Give Rules to Parser
            val transaction = NotificationTransactionParser.parse(title, text, customRules)

            // C. Save if Valid
            if (transaction != null) {
                val newTx = TransactionEntity(
                    amount = transaction.amount,
                    note = transaction.note,
                    category = transaction.category,
                    isIncome = transaction.isIncome,
                    date = System.currentTimeMillis()
                )
                repository.insert(newTx)
                sendSuccessNotification(transaction)
            }
        }
    }

    private fun sendSuccessNotification(tx: ParsedTransaction) {
        val channelId = "flux_success_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Flux Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        val amountString = format.format(tx.amount).replace("Rp", "Rp ")

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = "$amountString (${tx.category})"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.flux_transparent)
            .setColor("#0B0E14".toColorInt())
            .setContentTitle("Flux Recorded This! ✅")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}