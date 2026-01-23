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
        repository = TransactionRepository(database.transactionDao())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        // 1. FILTER CUMA BLU BY BCA
        val packageName = sbn.packageName

        // Filter ID Paket (Blu & Debug App Kita Sendiri)
//        if (packageName != "id.co.blu" && packageName != "com.example.flux") return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        // --- 🛡️ ANTI-LOOP PROTECTION 🛡️ ---
        // Kalau notifnya adalah notif konfirmasi dari kita sendiri, STOP DISINI.
        // Jangan diproses lagi biar ga looping.
        if (title.contains("Flux Recorded This")) return

        // --- MODE DEBUG: CATAT SEMUANYA ---
        // Kita ga pake Parser. Kita langsung bungkus mentah-mentah.

        serviceScope.launch {
            val debugNote = "[$packageName] $title: $text"

            // Potong kalo kepanjangan biar ga error database
            val safeNote = if (debugNote.length > 100) debugNote.take(100) + "..." else debugNote

            val newTx = TransactionEntity(
                amount = 0.0, // Nol Rupiah biar ga ngerusak grafik
                note = safeNote, // Isinya teks notifikasi asli
                category = "DEBUG_LOG", // Kategori khusus
                isIncome = false,
                date = System.currentTimeMillis()
            )
            repository.insert(newTx)

            Log.d("FluxListener", "DEBUG SAVED: $safeNote")
        }

        Log.d("FluxListener", "Processing: $title | $text")

        // 3. PARSING (Pake Logika Asli Blu)
        val transaction = NotificationTransactionParser.parse(title, text)

        if (transaction != null) {
            serviceScope.launch {
                val newTx = TransactionEntity(
                    amount = transaction.amount,
                    note = transaction.note,
                    category = transaction.category,
                    isIncome = transaction.isIncome,
                    date = System.currentTimeMillis()
                )
                repository.insert(newTx)

                // Kirim notif sukses (Ini yang nanti bakal diblokir sama Filter No. 1)
                sendSuccessNotification(transaction)
            }
        }

//        Log.d("FluxListener", "Notif Detected: $title | $text")
//
//        // 2. PARSING
//        val transaction = NotificationTransactionParser.parse(title, text)
//
//        if (transaction != null) {
//            serviceScope.launch {
//                val newTx = TransactionEntity(
//                    amount = transaction.amount,
//                    note = transaction.note,
//                    category = transaction.category,
//                    isIncome = transaction.isIncome,
//                    date = System.currentTimeMillis()
//                )
//                repository.insert(newTx)
//
//                // 3. SUKSES SIMPAN -> KIRIM NOTIFIKASI BALIK
//                sendSuccessNotification(transaction)
//            }
//        }
    }

    // --- FUNGSI BARU BUAT NGASIH TAU USER ---
    private fun sendSuccessNotification(tx: ParsedTransaction) {
        val channelId = "flux_success_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Bikin Channel (Wajib buat Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Flux Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Format Rupiah buat di notif
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        val amountString = format.format(tx.amount).replace("Rp", "Rp ")

        // Action: Kalau notif diklik, buka aplikasi Flux
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Teks Notifikasinya
        // Contoh: "Recorded: Rp 50.000 (Food)"
        val contentText = "$amountString (${tx.category})"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.flux_transparent) // Pake icon putih yang tadi
            .setColor("#0B0E14".toColorInt()) // Warna Teal Flux
            .setContentTitle("Flux Recorded This! ✅") // Judul Notif
            .setContentText(contentText) // Isi Notif
            .setContentIntent(pendingIntent) // Biar bisa diklik
            .setAutoCancel(true) // Ilang pas diklik
            .build()

        // Tampilkan (ID pake currentTimeMillis biar ga numpuk/ketimpa notif lama)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}