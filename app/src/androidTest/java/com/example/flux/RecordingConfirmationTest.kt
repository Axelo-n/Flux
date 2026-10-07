package com.example.flux

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.flux.data.*
import com.example.flux.notification.RecordingNotifications
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecordingConfirmationTest {
    @Test fun confirmationUsesCommittedMerchantAndDuplicatesDoNotConfirmAgain() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        val manager = context.getSystemService(NotificationManager::class.java)
        val db = Room.inMemoryDatabaseBuilder(context, TransactionDatabase::class.java).build()
        val repo = TransactionRepository(db)
        val eventId = "confirmation-test"
        try {
            val now = System.currentTimeMillis()
            val day = LocalDate.now(ZoneId.of("Asia/Jakarta")).toEpochDay()
            repo.start(FinanceConfig(startDay = day, timezone = "Asia/Jakarta", openingBalance = 1000000, activatedAt = now - 1000), BudgetPolicy(day, List(7) { 50000 }.joinToString(",")))
            val event = NotificationRecord(eventId, "test-fingerprint", now, "blu", "Transaksi di gorengan gembleng Rp 5000 berhasil", "", "")
            val parsed = repo.recordNotificationDetail(event)!!
            assertEquals("gorengan gembleng", parsed.note)
            RecordingNotifications.post(context, eventId, parsed)
            var notification = manager.activeNotifications.firstOrNull { it.id == RecordingNotifications.id(eventId) }
            for (attempt in 0..20) { if (notification != null) break; Thread.sleep(50); notification = manager.activeNotifications.firstOrNull { it.id == RecordingNotifications.id(eventId) } }
            assertNotNull(notification)
            assertTrue(notification!!.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("gorengan gembleng"))
            assertNull(repo.recordNotificationDetail(event))
            assertEquals(1, manager.activeNotifications.count { it.id == RecordingNotifications.id(eventId) })
            repo.insertRule(ParserRule(keyword = "blugether", targetCategory = "Other", blocked = true))
            assertNull(repo.recordNotificationDetail(event.copy(eventId = "blocked-test", occurrenceId = "blocked-test", fingerprint = "blocked-test", text = "Transaksi di blugether Rp 5000 berhasil")))
        } finally { manager.cancel(RecordingNotifications.id(eventId)); db.close() }
    }
}
