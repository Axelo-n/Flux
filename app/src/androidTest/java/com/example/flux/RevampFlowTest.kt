package com.example.flux

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.flux.data.*
import com.example.flux.ui.DashboardScreen
import com.example.flux.ui.theme.FluxTheme
import com.example.flux.viewmodel.DashboardViewModel
import org.junit.*
import java.io.File

class RevampFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: TransactionDatabase
    private lateinit var originalPreferences: com.example.flux.preferences.UiPreferences
    @Before fun preparePreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        com.example.flux.preferences.AppPreferences.initialize(context)
        originalPreferences = com.example.flux.preferences.AppPreferences.state.value
        com.example.flux.preferences.AppPreferences.language(context, "id")
        com.example.flux.preferences.AppPreferences.theme(context, "teal")
    }
    @After fun close() {
        if (::db.isInitialized) db.close()
        if (::originalPreferences.isInitialized) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            com.example.flux.preferences.AppPreferences.language(context, originalPreferences.language)
            com.example.flux.preferences.AppPreferences.theme(context, originalPreferences.theme)
        }
    }
    private fun screenshot(name: String) {
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Allow system toasts and keyboard dismissal animations to finish.
        Thread.sleep(3500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "flux-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun pocketAndCashbackSelection() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TransactionDatabase::class.java).build()
        val repository = TransactionRepository(db)
        kotlinx.coroutines.runBlocking {
            val now = System.currentTimeMillis()
            val today = com.example.flux.model.BudgetEngine.day(now, "Asia/Jakarta").toEpochDay()
            repository.start(FinanceConfig(startDay = today, openingBalance = 1000000, activatedAt = 0), BudgetPolicy(today, List(7) { 50000 }.joinToString(",")))
            repository.insert(TransactionEntity(amount = 8000.0, note = "Uji cashback", category = "Snack", isIncome = false, date = now - 1000))
            repository.recordNotification(NotificationRecord("ui-cashback", "ui-cashback", now, "Cashback diterima", "Cashback Rp 2.000 berhasil diterima", "", ""))
        }
        val vm = DashboardViewModel(repository)
        compose.setContent { FluxTheme { DashboardScreen(vm) } }
        compose.waitUntil(10000) { vm.uiState.value.configured && vm.pendingCashbacks.value.size == 1 }
        compose.onNodeWithText("Sistem").performClick()
        compose.onNodeWithText("Budget pocket").performScrollTo().performClick()
        compose.onNodeWithText("DANA BULAN INI").assertExists()
        val first = compose.onNodeWithText("Pocket minggu 1").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Pocket minggu 2").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue(second.left > first.right)
        screenshot("pockets")
        compose.onNodeWithText("Pocket minggu 1").performClick()
        compose.onNodeWithText("Rincian harian").assertExists()
        screenshot("pocket-detail")
        compose.onNodeWithContentDescription("Tutup").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Tambah"))
        compose.onNodeWithText("Tambah").performClick()
        compose.onNodeWithText("Nama pocket").performTextInput("Uang kos")
        compose.onNodeWithText("Nominal per bulan").performTextInput("1500000")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Simpan pocket").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.monthlyPockets.value.size == 1 }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Uang kos"))
        compose.onNodeWithText("Uang kos").performClick()
        compose.onNodeWithText("Edit pocket").performClick()
        compose.onNodeWithText("Nominal per bulan").performTextClearance()
        compose.onNodeWithText("Nominal per bulan").performTextInput("1600000")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Simpan pocket").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.monthlyPockets.value.single().amount == 1600000L }
        screenshot("pockets-manual")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Uang kos"))
        compose.onNodeWithText("Uang kos").performClick()
        compose.onNodeWithText("Hapus pocket").performClick()
        compose.onNodeWithText("Hapus pocket").performClick()
        compose.waitUntil(10000) { vm.monthlyPockets.value.isEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("Kembali").performClick()
        compose.onNodeWithText("Pilih transaksi asal").performScrollTo()
        compose.onRoot().performTouchInput { swipeUp(startY = height * .7f, endY = height * .4f) }
        compose.onNodeWithText("Pilih transaksi asal").performClick()
        compose.onNodeWithText("Uji cashback").assertExists()
        screenshot("cashback-dialog")
        compose.onNodeWithText("Uji cashback").performClick()
        compose.waitUntil(10000) { vm.pendingCashbacks.value.isEmpty() && vm.uiState.value.recentTransactions.single().refund == 2000.0 }
    }
    @Test fun filtersSearchAndSamplePreview() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TransactionDatabase::class.java).build()
        val repository = TransactionRepository(db)
        kotlinx.coroutines.runBlocking {
            val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")).toEpochDay()
            repository.start(FinanceConfig(startDay = today, openingBalance = 1000000, activatedAt = 0), BudgetPolicy(today, List(7) { 50000 }.joinToString(",")))
            repository.insertRule(ParserRule(keyword = "warung budi", targetCategory = "Lunch", targetNote = "Nasgor"))
            repository.insert(TransactionEntity(amount = 8000.0, note = "Sarapan uji", category = "Breakfast", isIncome = false))
            repository.insert(TransactionEntity(amount = 100000.0, note = "Dana uji", category = "Income", isIncome = true))
        }
        val vm = DashboardViewModel(repository)
        compose.setContent { FluxTheme { DashboardScreen(vm) } }
        compose.waitUntil(10000) { vm.uiState.value.configured && vm.uiState.value.recentTransactions.size == 2 }
        compose.onAllNodesWithText("Riwayat").onLast().performClick()
        compose.onNode(hasText("Pemasukan") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)).performClick()
        compose.onNodeWithText("Dana uji").assertExists()
        compose.onNodeWithText("Sarapan uji").assertDoesNotExist()
        compose.onNodeWithText("Semua").performClick()
        compose.onNodeWithText("Cari catatan atau kategori").performTextInput("Sarapan")
        compose.onNodeWithText("Sarapan uji").assertExists()
        compose.onNodeWithText("Dana uji").assertDoesNotExist()
        screenshot("history-controls")
        compose.onNodeWithContentDescription("Hapus pencarian").performClick()
        compose.onNodeWithText("Dana uji").assertExists()
        compose.onNodeWithText("Sistem").performClick()
        compose.onNodeWithText("Aturan otomatis & blacklist").performClick()
        compose.onNodeWithText("Blacklist (0)").performClick()
        compose.onNodeWithText("warung budi").assertDoesNotExist()
        compose.onNodeWithText("Parser (1)").performClick()
        compose.onNodeWithText("Cari keyword atau catatan").performTextInput("tidakada")
        compose.onNodeWithText("warung budi").assertDoesNotExist()
        compose.onNodeWithContentDescription("Hapus pencarian").performClick()
        compose.onNodeWithText("warung budi").assertExists()
        screenshot("rule-controls")
        compose.onNodeWithText("Uji contoh notifikasi").performScrollTo().performClick()
        compose.onNodeWithText("Pakai contoh blu").performClick()
        compose.onAllNodesWithText("Nasgor").onLast().assertIsDisplayed()
        screenshot("sample-notification")
        Assert.assertEquals(2, kotlinx.coroutines.runBlocking { repository.getAllTransactionsSync().size })
    }
    @Test fun categoryPickerUsesUniqueIconsAndKeepsSelection() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TransactionDatabase::class.java).build()
        val repository = TransactionRepository(db)
        kotlinx.coroutines.runBlocking {
            val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")).toEpochDay()
            repository.start(FinanceConfig(startDay = today, openingBalance = 1000000, activatedAt = 0), BudgetPolicy(today, List(7) { 50000 }.joinToString(",")))
        }
        val visuals = com.example.flux.ui.theme.categoryVisuals
        Assert.assertEquals(14, visuals.size)
        Assert.assertEquals(14, visuals.map { it.icon }.distinct().size)
        visuals.forEach { Assert.assertNotNull(androidx.core.content.ContextCompat.getDrawable(InstrumentationRegistry.getInstrumentation().targetContext, it.icon)) }
        val vm = DashboardViewModel(repository)
        compose.setContent { FluxTheme { DashboardScreen(vm) } }
        compose.waitUntil(10000) { vm.uiState.value.configured }
        compose.onNodeWithContentDescription("Catat transaksi").performClick()
        compose.onNodeWithContentDescription("Ubah kategori").performScrollTo().performClick()
        val first = compose.onNodeWithContentDescription("Pilih kategori Sarapan").fetchSemanticsNode().boundsInRoot
        val third = compose.onNodeWithContentDescription("Pilih kategori Makan malam").fetchSemanticsNode().boundsInRoot
        Assert.assertEquals(first.top, third.top, 1f)
        Assert.assertTrue(third.left > first.right)
        screenshot("category-picker")
        compose.onNodeWithContentDescription("Pilih kategori Minuman").performClick()
        compose.onNodeWithText("Pilih kategori").assertDoesNotExist()
        compose.onNodeWithText("Minuman").assertExists()
        screenshot("transaction-compact-category")
        compose.onNodeWithText("Nominal").performScrollTo().performTextInput("3500")
        compose.onNodeWithText("Catatan · opsional").performScrollTo().performTextInput("Uji minuman")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Simpan transaksi").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.uiState.value.recentTransactions.singleOrNull()?.category == "Drinks" }
        val row = kotlinx.coroutines.runBlocking { repository.getAllTransactionsSync().single() }
        Assert.assertEquals("Uji minuman", row.note)
        Assert.assertEquals(3500.0, row.amount, 0.0)
        Assert.assertEquals(com.example.flux.R.drawable.ic_category_drinks, vm.uiState.value.recentTransactions.single().iconRes)
    }
    @Test fun setupManualExpenseRefundAndRuleEditor() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TransactionDatabase::class.java).build()
        val vm = DashboardViewModel(TransactionRepository(db))
        compose.setContent { FluxTheme { DashboardScreen(vm) } }
        compose.waitUntil(10000) { !vm.uiState.value.isLoading }
        compose.onNodeWithText("Saldo blu saat ini (Rp)").assertExists()
        screenshot("setup")
        compose.onNodeWithText("Saldo blu saat ini (Rp)").performTextInput("1000000")
        compose.onNodeWithText("Mulai pencatatan baru").performScrollTo().performClick()
        compose.onNodeWithText("Hapus & mulai").performClick()
        compose.waitUntil(10000) { vm.uiState.value.configured }
        compose.onNodeWithText("SISA JATAH HARI INI").assertExists()
        screenshot("home")
        compose.onNodeWithText("Analisis").performClick()
        compose.onNodeWithText("Tren harian").performScrollTo()
        compose.onNodeWithTag("trend-today").assertIsDisplayed()
        screenshot("analytics")
        compose.onNodeWithText("Hari ini").performClick()
        compose.onNodeWithContentDescription("Catat transaksi").performClick()
        compose.onNodeWithText("Nominal").performTextInput("40000")
        screenshot("transaction")
        compose.onNodeWithText("Simpan transaksi").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.uiState.value.recentTransactions.size == 1 }
        screenshot("home-filled")
        compose.onAllNodesWithText("Riwayat").onLast().performClick()
        compose.onNodeWithText(com.example.flux.preferences.translate(vm.uiState.value.recentTransactions.single().category), substring = false).performClick()
        compose.onNodeWithText("Tambahkan refund / cashback").performScrollTo().performClick()
        compose.onNodeWithText("Refund penuh").performScrollTo().performClick()
        compose.onNodeWithText("Alasan refund / cashback · wajib").performScrollTo().performTextInput("Pesanan dibatalkan")
        compose.onNodeWithText("Simpan transaksi").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.uiState.value.recentTransactions.singleOrNull()?.amount == 0.0 }
        compose.onNodeWithText("Batal · Pesanan dibatalkan").assertExists()
        screenshot("history")
        compose.onNodeWithText("Sistem").performClick()
        screenshot("system")
        compose.onNodeWithText("Budget harian").performScrollTo().performClick()
        screenshot("budget")
        compose.onNodeWithText("Senin").performTextClearance()
        compose.onNodeWithText("Senin").performTextInput("60000")
        compose.onNodeWithText("Jadwalkan budget").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.policies.value.size == 2 }
        Assert.assertEquals(60000L, vm.policies.value.last().weeklyAmounts().first())
        compose.onNodeWithText("Aturan otomatis & blacklist").performClick()
        compose.onNodeWithText("+ Tambah aturan").performClick()
        screenshot("rule-dialog")
        compose.onNodeWithText("Keyword · contoh: nama penjual").performTextInput("warung budi")
        compose.onNodeWithText("Catatan otomatis · opsional").performTextInput("Nasgor")
        compose.onNodeWithText("Simpan").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.parserRules.value.size == 1 }
        compose.onNodeWithText("warung budi").assertExists()
        screenshot("rules")
        compose.onNodeWithText("Kembali").performClick()
        for ((name, key) in listOf("Putih" to "white", "Hitam" to "black", "Teal" to "teal", "Luca" to "luca", "Violet" to "violet")) {
            compose.onNodeWithText("Tema").performScrollTo().performClick()
            if (key == "white") screenshot("theme-dialog")
            compose.onNodeWithText(name).performClick()
            compose.waitUntil { com.example.flux.preferences.AppPreferences.state.value.theme == key }
            screenshot("theme-$key")
            if (key == "luca") {
                compose.onNodeWithText("Hari ini").performClick()
                screenshot("luca-home")
                compose.onNodeWithText("Analisis").performClick()
                screenshot("luca-analytics")
                compose.onNodeWithText("Sistem").performClick()
            }
        }
        compose.onNodeWithText("Bahasa").performScrollTo().performClick()
        screenshot("language-dialog")
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Preferences").assertExists()
        compose.onNodeWithText("Language").assertExists()
        Assert.assertEquals("en", InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("flux_appearance", 0).getString("language", ""))
        Assert.assertEquals("violet", InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("flux_appearance", 0).getString("theme", ""))
        screenshot("english-system")
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("DAILY BUDGET LEFT").assertExists()
        screenshot("english-home")
        val preferencesContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        com.example.flux.preferences.AppPreferences.language(preferencesContext, "id")
        com.example.flux.preferences.AppPreferences.theme(preferencesContext, "teal")
    }
}
