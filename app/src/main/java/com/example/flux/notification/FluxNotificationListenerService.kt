package com.example.flux.notification

import android.app.*
import android.content.*
import android.os.Build
import android.service.notification.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flux.preferences.AppPreferences
import com.example.flux.preferences.translate
import com.example.flux.MainActivity
import com.example.flux.R
import com.example.flux.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

data class ListenerHealth(val connected: Boolean = false, val lastConnected: Long = 0, val lastEvent: Long = 0, val error: String? = null)

class FluxNotificationListenerService : NotificationListenerService() {
    companion object {
        private val state = MutableStateFlow(ListenerHealth())
        val health = state.asStateFlow()
        fun reconnect(context: Context) {
            runCatching { requestRebind(ComponentName(context, FluxNotificationListenerService::class.java)) }.onFailure {
                state.value = state.value.copy(connected = false, error = "Belum bisa terhubung. Periksa akses notifikasi di Android.")
            }
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: TransactionRepository
    override fun onCreate() {
        super.onCreate()
        AppPreferences.initialize(this)
        repository = TransactionRepository(TransactionDatabase.getDatabase(applicationContext))
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        state.value = state.value.copy(connected = true, lastConnected = System.currentTimeMillis(), error = null)
        try { foreground() } catch (e: Exception) {
            Log.w("FluxListener", "Foreground unavailable", e)
            state.value = state.value.copy(error = "Notifikasi layanan belum aktif. Periksa izin notifikasi.")
        }
        try { activeNotifications?.forEach { process(it) } } catch (e: Exception) { Log.w("FluxListener", "Recovery unavailable", e) }
    }
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        state.value = state.value.copy(connected = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        reconnect(this)
    }
    override fun onDestroy() {
        state.value = state.value.copy(connected = false)
        scope.cancel()
        super.onDestroy()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { sbn?.let { process(it) } }
    private fun process(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.bcadigital.blu") return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        state.value = state.value.copy(lastEvent = System.currentTimeMillis())
        scope.launch {
            try {
                val fingerprint = hash("${sbn.key}|$title|$text")
                val occurrence = hash("${sbn.key}|${sbn.postTime}")
                val eventId = hash("$occurrence|$fingerprint")
                val recorded = repository.recordNotificationDetail(NotificationRecord(eventId, fingerprint, sbn.postTime, title, text, "", "", occurrence))
                if (recorded != null) runCatching { RecordingNotifications.post(this@FluxNotificationListenerService, eventId, recorded) }.onFailure { Log.w("FluxListener", "Confirmation notification unavailable", it) }
            } catch (e: Exception) {
                Log.e("FluxListener", "Recording failed", e)
                state.value = state.value.copy(error = "Pencatatan gagal. Periksa riwayat dan koreksi manual.")
            }
        }
    }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun foreground() {
        val channel = "flux_persistent_channel"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channel, translate("Pencatatan otomatis"), NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        startForeground(1999, NotificationCompat.Builder(this, channel).setSmallIcon(R.drawable.ic_wallet_outline).setContentTitle(translate("Flux · blu terhubung")).setContentText(translate("Pencatatan otomatis siap. Ketuk untuk melihat keuangan.")).setContentIntent(intent).setOngoing(true).setSilent(true).build())
    }
}
