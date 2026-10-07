package com.example.flux.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.flux.MainActivity
import com.example.flux.R
import com.example.flux.preferences.translate
import com.example.flux.viewmodel.rupiah

object RecordingNotifications {
    fun id(eventId: String) = (eventId.hashCode() and 0x3fffffff) + 2000
    fun post(context: Context, eventId: String, parsed: ParsedTransaction) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val channel = "flux_recorded_channel"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channel, translate("Transaksi tercatat"), NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val summary = "${translate(if (parsed.isIncome) "Pemasukan" else "Pengeluaran")} · ${rupiah(parsed.amount.toLong())} · ${parsed.note.ifBlank { translate(parsed.category) }}"
        manager.notify(id(eventId), NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_wallet_outline).setContentTitle(translate("Transaksi tercatat")).setContentText(summary).setStyle(NotificationCompat.BigTextStyle().bigText(summary)).setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true).build())
    }
}
