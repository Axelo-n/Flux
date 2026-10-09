package com.example.flux

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.flux.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: TransactionDatabase
    private lateinit var repo: TransactionRepository
    private val today get() = LocalDate.now(ZoneId.of("Asia/Jakarta")).toEpochDay()
    @Before fun prepare() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TransactionDatabase::class.java).build()
        repo = TransactionRepository(db)
        repo.start(FinanceConfig(startDay = today, openingBalance = 1_000_000, activatedAt = 0), BudgetPolicy(today, List(7) { 50000 }.joinToString(",")))
    }
    @After fun close() { db.close() }
    private fun event(id: String, text: String = "Rp 40.000 seller", fingerprint: String = "fingerprint", time: Long = System.currentTimeMillis(), occurrence: String = id) = NotificationRecord(id, fingerprint, time, "Pembayaran berhasil", text, "", "", occurrence)
    @Test fun monthlyPocketsPersistAndBackupWithoutChangingMoney() = runBlocking {
        repo.saveMonthlyPocket(MonthlyPocket(name = "Uang kos", amount = 1500000))
        val pocket = db.financeDao().monthlyPockets().single()
        val backup = repo.backup()
        assertEquals(1500000L, backup.monthlyPockets!!.single().amount)
        assertEquals(1000000L, backup.config!!.openingBalance)
        assertTrue(backup.transactions.isEmpty())
        repo.saveMonthlyPocket(pocket.copy(amount = 1600000))
        assertEquals(1600000L, db.financeDao().monthlyPockets().single().amount)
        repo.deleteMonthlyPocket(pocket)
        assertTrue(db.financeDao().monthlyPockets().isEmpty())
        val json = com.google.gson.Gson().toJson(backup)
        repo.restoreData(com.google.gson.Gson().fromJson(json, FluxBackupData::class.java))
        assertEquals(pocket, db.financeDao().monthlyPockets().single())
        assertTrue(repo.getAllTransactionsSync().isEmpty())
        assertTrue(runCatching { repo.saveMonthlyPocket(pocket.copy(amount = -1)) }.isFailure)
        assertTrue(runCatching { repo.restoreData(backup.copy(monthlyPockets = listOf(pocket.copy(amount = -1)))) }.isFailure)
        assertEquals(pocket, db.financeDao().monthlyPockets().single())
    }
    @Test fun olderBackupWithoutPocketsStillRestores() = runBlocking {
        val json = com.google.gson.JsonParser.parseString(com.google.gson.Gson().toJson(repo.backup())).asJsonObject
        json.remove("monthlyPockets")
        repo.saveMonthlyPocket(MonthlyPocket(name = "Tabungan", amount = 100000))
        repo.restoreData(com.google.gson.Gson().fromJson(json, FluxBackupData::class.java))
        assertTrue(db.financeDao().monthlyPockets().isEmpty())
        assertEquals(1000000L, db.financeDao().config()!!.openingBalance)
    }
    @Test fun cashbackRestoresBalanceAndBudgetWithoutAddingIncome() = runBlocking {
        val now = System.currentTimeMillis()
        repo.insert(TransactionEntity(amount = 40000.0, note = "Warung Budi", category = "Lunch", isIncome = false, date = now - 1000))
        val input = event("cashback", "Cashback Rp 5.000 dari Warung Budi berhasil", "cashback-fingerprint", now).copy(title = "Cashback diterima")
        assertTrue(repo.recordNotificationDetail(input)!!.isCashback)
        val row = repo.getAllTransactionsSync().single()
        assertEquals(5000.0, row.refund, 0.0)
        assertTrue(row.refundNote.contains("Cashback"))
        val totals = com.example.flux.model.BudgetEngine.calculate(db.financeDao().config(), db.financeDao().policies(), listOf(row), emptyList(), LocalDate.ofEpochDay(today))
        assertEquals(965000L, totals.balance); assertEquals(15000L, totals.remaining)
        assertFalse(repo.recordNotification(input))
        assertTrue(runCatching { repo.linkCashback(input.eventId, row.id) }.isFailure)
    }
    @Test fun ambiguousCashbackWaitsForSelectionAndDuplicateCopiesCloseTogether() = runBlocking {
        val now = System.currentTimeMillis()
        repeat(2) { repo.insert(TransactionEntity(amount = 40000.0, note = "Warung Budi", category = "Lunch", isIncome = false, date = now - 2000 + it)) }
        val input = event("cashback", "Cashback Rp 5.000 dari Warung Budi berhasil", "cashback-fingerprint", now, "cashback-occurrence").copy(title = "Cashback diterima")
        assertNull(repo.recordNotificationDetail(input))
        val copy = input.copy(eventId = "cashback-update", fingerprint = "changed-payload")
        assertNull(repo.recordNotificationDetail(copy))
        assertTrue(repo.getAllTransactionsSync().all { it.refund == 0.0 })
        val chosen = repo.getAllTransactionsSync().first()
        repo.linkCashback(input.eventId, chosen.id)
        assertEquals(5000.0, repo.getAllTransactionsSync().sumOf { it.refund }, 0.0)
        assertEquals("Diabaikan", db.financeDao().notification(copy.eventId)!!.status)
        assertTrue(runCatching { repo.linkCashback(copy.eventId, chosen.id) }.isFailure)
    }
    @Test fun cashbackOnEarlierDayRestoresExtraAndRejectsOverRefund() = runBlocking {
        val yesterday = LocalDate.ofEpochDay(today).minusDays(1)
        val yesterdayTime = yesterday.atTime(12, 0).atZone(ZoneId.of("Asia/Jakarta")).toInstant().toEpochMilli()
        repo.start(FinanceConfig(startDay = yesterday.toEpochDay(), openingBalance = 1000000, activatedAt = 0), BudgetPolicy(yesterday.toEpochDay(), List(7) { 50000 }.joinToString(",")))
        repo.insert(TransactionEntity(amount = 40000.0, note = "", category = "Lunch", isIncome = false, date = yesterdayTime))
        val input = event("cashback", "Cashback Rp 5.000 berhasil diterima", "cashback-fingerprint").copy(title = "Cashback diterima")
        assertNull(repo.recordNotificationDetail(input))
        val row = repo.getAllTransactionsSync().single()
        repo.linkCashback(input.eventId, row.id)
        val totals = com.example.flux.model.BudgetEngine.calculate(db.financeDao().config(), db.financeDao().policies(), repo.getAllTransactionsSync(), emptyList(), LocalDate.ofEpochDay(today))
        assertEquals(15000L, totals.extra); assertEquals(50000L, totals.remaining)
        val excessive = input.copy(eventId = "too-much", occurrenceId = "too-much", fingerprint = "too-much", text = "Cashback Rp 50.000 berhasil diterima")
        assertNull(repo.recordNotificationDetail(excessive))
        assertTrue(runCatching { repo.linkCashback(excessive.eventId, row.id) }.isFailure)
        assertEquals(5000.0, repo.getAllTransactionsSync().single().refund, 0.0)
    }
    @Test fun manuallyHandledCashbackNeverChangesMoney() = runBlocking {
        val input = event("manual-cashback", "Cashback Rp 5.000 berhasil diterima", "manual-cashback").copy(title = "Cashback diterima")
        assertNull(repo.recordNotificationDetail(input))
        repo.dismissCashback(input.eventId)
        assertTrue(repo.getAllTransactionsSync().isEmpty())
        assertFalse(repo.recordNotification(input.copy(eventId = "manual-update")))
    }
    @Test fun duplicateReconnectAndUpdateOnlyRecordOnce() = runBlocking {
        assertTrue(repo.recordNotification(event("one", occurrence = "same")))
        assertFalse(repo.recordNotification(event("one", occurrence = "same")))
        assertFalse(repo.recordNotification(event("two", fingerprint = "changed", occurrence = "same")))
        assertFalse(repo.recordNotification(event("three")))
        assertEquals(1, repo.getAllTransactionsSync().size)
    }
    @Test fun incompleteUpdateCanBecomeARecordedTransaction() = runBlocking {
        assertFalse(repo.recordNotification(event("one", "Belum ada nominal", "first", occurrence = "same")))
        assertTrue(repo.recordNotification(event("two", fingerprint = "complete", occurrence = "same")))
        assertEquals(1, repo.getAllTransactionsSync().size)
    }
    @Test fun blacklistAndStaleNotificationsNeverInsertMoney() = runBlocking {
        repo.insertRule(ParserRule(keyword = "blugether", targetCategory = "", blocked = true))
        assertFalse(repo.recordNotification(event("blocked", "Rp 40.000 blugether")))
        db.financeDao().saveConfig(FinanceConfig(startDay = today, openingBalance = 0, activatedAt = System.currentTimeMillis()))
        assertFalse(repo.recordNotification(event("stale", time = 1)))
        assertTrue(repo.getAllTransactionsSync().isEmpty())
    }
    @Test fun deletedTransactionIsNotResurrectedByReconnect() = runBlocking {
        val input = event("one")
        repo.recordNotification(input)
        repo.delete(repo.getAllTransactionsSync().single().id)
        assertFalse(repo.recordNotification(input))
        assertTrue(repo.getAllTransactionsSync().isEmpty())
    }
    @Test fun invalidRestoreLeavesExistingDataIntact() = runBlocking {
        repo.insert(TransactionEntity(amount = 40000.0, note = "", category = "Other", isIncome = false))
        val backup = repo.backup()
        val broken = backup.copy(transactions = listOf(backup.transactions.single().copy(refund = 50000.0)))
        assertTrue(runCatching { repo.restoreData(broken) }.isFailure)
        assertEquals(40000.0, repo.getAllTransactionsSync().single().amount, 0.0)
        assertEquals(1_000_000L, db.financeDao().config()!!.openingBalance)
        repo.restoreData(backup)
        assertEquals(1, repo.getAllTransactionsSync().size)
    }
    @Test fun fullRefundRetainsOriginalAndAllowsEmptyNote() = runBlocking {
        repo.insert(TransactionEntity(amount = 40000.0, note = "", category = "Other", isIncome = false, refund = 40000.0, refundNote = "Pesanan dibatalkan"))
        val row = repo.getAllTransactionsSync().single()
        assertEquals("", row.note); assertEquals(0.0, row.amount - row.refund, 0.0)
    }
    @Test fun restoreDatabaseFailureRollsBackAllTables() = runBlocking {
        repo.insert(TransactionEntity(amount = 40000.0, note = "original", category = "Other", isIncome = false))
        val backup = repo.backup()
        val correction = BalanceAdjustment(id = 1, target = "saldo", amount = 10000, note = "restore", date = System.currentTimeMillis())
        val bad = backup.copy(transactions = listOf(backup.transactions.single().copy(note = "replacement")), adjustments = listOf(correction, correction))
        assertTrue(runCatching { repo.restoreData(bad) }.isFailure)
        assertEquals("original", repo.getAllTransactionsSync().single().note)
        assertTrue(db.financeDao().adjustments().isEmpty())
    }
    @Test fun pastBudgetIsImmutableAndFutureScheduleCanBeUpdatedOrCancelled() = runBlocking {
        assertTrue(runCatching { repo.addPolicy(BudgetPolicy(today, List(7) { 75000 }.joinToString(","))) }.isFailure)
        val future = BudgetPolicy(today + 1, List(7) { 75000 }.joinToString(","))
        repo.addPolicy(future)
        repo.addPolicy(future.copy(amounts = List(7) { 80000 }.joinToString(",")))
        assertEquals(2, db.financeDao().policies().size)
        assertEquals(80000L, db.financeDao().policies().last().weeklyAmounts()[0])
        assertTrue(runCatching { repo.deleteFuturePolicy(today) }.isFailure)
        repo.deleteFuturePolicy(today + 1)
        assertEquals(1, db.financeDao().policies().size)
    }
    @Test fun oldDatabaseMigratesWithoutDeletingExistingMoney() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "flux-migration-test.sqlite"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            sql.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount REAL NOT NULL, note TEXT NOT NULL, category TEXT NOT NULL, isIncome INTEGER NOT NULL, date INTEGER NOT NULL)")
            sql.execSQL("CREATE TABLE parser_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, keyword TEXT NOT NULL, targetCategory TEXT NOT NULL, targetNote TEXT)")
            sql.execSQL("INSERT INTO transactions VALUES (1, 40000, 'old', 'Other', 0, 1)")
            sql.version = 2
        }
        val migrated = Room.databaseBuilder(context, TransactionDatabase::class.java, name).addMigrations(TransactionDatabase.MIGRATION_2_3, TransactionDatabase.MIGRATION_3_4).build()
        try { assertEquals("old", migrated.transactionDao().getAllTransactionsSync().single().note); assertNull(migrated.financeDao().config()) }
        finally { migrated.close(); context.deleteDatabase(name) }
    }
}
