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
        compose.onNodeWithText("Makanan & minuman", substring = false).performClick()
        compose.onNodeWithText("Tambahkan refund").performScrollTo().performClick()
        compose.onNodeWithText("Refund penuh").performScrollTo().performClick()
        compose.onNodeWithText("Alasan refund / cancel · wajib").performScrollTo().performTextInput("Pesanan dibatalkan")
        compose.onNodeWithText("Simpan transaksi").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.uiState.value.recentTransactions.singleOrNull()?.amount == 0.0 }
        compose.onNodeWithText("Batal · Pesanan dibatalkan").assertExists()
        screenshot("history")
        compose.onNodeWithText("Sistem").performClick()
        screenshot("system")
        compose.onNodeWithText("Budget harian").performClick()
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
