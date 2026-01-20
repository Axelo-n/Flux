package com.example.flux

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.flux.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        // Load data beneran dari Database
        observeDatabase()
    }

    // Fungsi buat Simpan Transaksi Baru (Dipanggil dari UI)
    fun addTransaction(amount: Double, note: String, category: String, isIncome: Boolean) {
        viewModelScope.launch {
            val newTx = TransactionEntity(
                amount = amount,
                note = note,
                category = category,
                isIncome = isIncome
            )
            repository.insert(newTx)
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            // Kita pantau terus perubahan di list transaksi
            repository.allTransactions.collect { txList ->

                // 1. Convert Entity (Database) ke UI Model (Tampilan)
                val uiTransactions = txList.map { entity ->
                    val (iconId, color) = getCategoryStyle(entity.category, entity.isIncome)

                    Transaction(
                        id = entity.id,
                        title = entity.note.ifEmpty { entity.category },
                        category = entity.category,
                        amount = entity.amount,
                        formattedAmount = formatRupiah(entity.amount),
                        iconRes = iconId,
                        iconBgColor = color,
                        isIncome = entity.isIncome,
                        date = entity.date
                    )
                }

                // 2. HITUNG ANALYTICS (Panggil Otak Pintar Kita Disini! 🧠)
                // Ini yang kemarin belum dipanggil, makanya datanya ngaco
                val analytics = calculateAnalytics(uiTransactions)

                // 3. Update UI State dengan Data Real dari Analytics
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        recentTransactions = uiTransactions,

                        // TIMPA DATA DUMMY DENGAN HASIL HITUNGAN ASLI:
                        dailyBudgetLeft = analytics.dailyLeft,
                        dailyUsagePercent = analytics.dailyUsagePercent,
                        currentBalance = analytics.currentBalance,
                        extraBalance = analytics.extraBalance,
                        isExtraBalancePositive = analytics.isExtraPositive,
                        graphData = analytics.graphData
                    )
                }
            }
        }
    }

    // 1. Fungsi Update Data ke Database
    fun updateTransaction(id: Int, amount: Double, note: String, category: String, isIncome: Boolean, date: Long) {
        viewModelScope.launch {
            val updateTx = TransactionEntity(
                id = id, // ID lama wajib dibawa biar dia tau mana yang ditimpa
                amount = amount,
                note = note,
                category = category,
                isIncome = isIncome,
                // Kita pertahankan tanggal lama (kalau mau update tanggal jadi "sekarang", ganti jadi System.currentTimeMillis())
                date = date
            )
            repository.insert(updateTx) // Di Room, @Insert(onConflict = REPLACE) itu otomatis jadi UPDATE kalau ID-nya sama
        }
    }

    // 2. Fungsi Helper buat Nyari Data (Dipake di Edit Screen)
    fun getTransactionById(id: Int): Transaction? {
        // Kita cari di list yang sekarang lagi tampil di layar (recentTransactions)
        return _uiState.value.recentTransactions.find { it.id == id }
    }

    // Helper: Format Rupiah
    private fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    private fun getCategoryStyle(category: String, isIncome: Boolean): Pair<Int, Color> {
        // 1. Kalau Income, iconnya Wallet warna Biru (atau sesuaikan)
        if (isIncome) {
            return Pair(R.drawable.ic_wallet_outline, CatBlue)
        }

        // 2. Kalau Expense, Cek String Kategorinya (HARUS SAMA PERSIS SAMA ADD SCREEN)
        return when (category) {
            "Food and Beverages" -> Pair(R.drawable.ic_food_outline, CatOrange)
            "Transportation" -> Pair(R.drawable.ic_car_outline, CatGreen)
            "Groceries and Shopping" -> Pair(R.drawable.ic_cart_outline, CatPurple)
            "Entertainment" -> Pair(R.drawable.ic_ticket_outline, CatYellow)
            "Account Transfer" -> Pair(R.drawable.ic_card_outline, CatBlue) // Atau icon transfer lain
            "Other" -> Pair(R.drawable.ic_other_outline, CatGrey)

            // Fallback kalau nama kategori ga dikenali
            else -> Pair(R.drawable.ic_other_outline, CatGrey)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    // --- LOGIC BUDGETING & ANALYTICS ---

    fun calculateAnalytics(transactions: List<Transaction>): AnalyticsState {
        val calendar = Calendar.getInstance()

        // 1. Tentukan Tanggal Hari Ini
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        // 2. Variable Penampung
        var totalIncomeForCurrent = 0.0 // Income khusus buat Current Balance
        var totalExpense = 0.0
        var extraBalance = 0.0

        // Grouping transaksi per hari
        val txByDay = transactions.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
        }

        // 3. Loop Kalkulasi
        txByDay.forEach { (_, txList) ->
            val txDate = Calendar.getInstance().apply { timeInMillis = txList.first().date }
            val dayOfWeek = txDate.get(Calendar.DAY_OF_WEEK)
            val dailyLimit = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) 60000.0 else 40000.0

            // --- PERUBAHAN DISINI (PEMISAHAN LOGIC) ---

            // A. Income untuk Current Balance (Semua Income KECUALI Injection_Extra)
            // Jadi kalo kita suntik Extra, Current Balance ga bakal naik.
            val incomeForCurrent = txList.filter {
                it.isIncome && it.category != "Injection_Extra"
            }.sumOf { it.amount }

            // B. Income untuk Extra Balance (Semua Income KECUALI Injection_Current)
            // Jadi kalo kita suntik Current, Extra Balance ga bakal naik.
            val incomeForExtra = txList.filter {
                it.isIncome && it.category != "Injection_Current"
            }.sumOf { it.amount }

            // Expense Harian
            val daysExpense = txList.filter { !it.isIncome }.sumOf { it.amount }

            // Update Total Global (Buat Current Balance nanti)
            totalIncomeForCurrent += incomeForCurrent
            totalExpense += daysExpense

            // Logic Extra Balance: Pakai incomeForExtra
            extraBalance += incomeForExtra

            // Logic Sisa Budget Harian (Tetap pakai limit - expense)
            val dailyLeft = dailyLimit - daysExpense

            // Kalo minus, potong Extra Balance
            if (dailyLeft < 0) {
                extraBalance += dailyLeft
            }
        }

        // 4. Hitung Data Hari Ini
        val dayOfWeekToday = calendar.get(Calendar.DAY_OF_WEEK)
        val limitToday = if (dayOfWeekToday == Calendar.SATURDAY || dayOfWeekToday == Calendar.SUNDAY) 60000.0 else 40000.0

        val todayTx = transactions.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_YEAR) == todayDay && c.get(Calendar.YEAR) == todayYear
        }
        val expenseToday = todayTx.filter { !it.isIncome }.sumOf { it.amount }
        val dailyLeftToday = limitToday - expenseToday

        // 5. Hitung Daily Percent
        val dailyUsagePercent = (expenseToday / limitToday).toFloat().coerceIn(0f, 1f)

        // 6. Siapkan Data Graph
        val graphData = (0..6).map { i ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -((6 - i)))
            val dKey = "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
            val dOfWeek = c.get(Calendar.DAY_OF_WEEK)
            val dLimit = if (dOfWeek == Calendar.SATURDAY || dOfWeek == Calendar.SUNDAY) 60000f else 40000f

            val tList = txByDay[dKey] ?: emptyList()
            val dExpense = tList.filter { !it.isIncome }.sumOf { it.amount }.toFloat()
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(c.time)

            DayData(dayLabel, dExpense, dLimit)
        }

        return AnalyticsState(
            dailyLeft = formatRupiah(dailyLeftToday),
            dailyUsagePercent = dailyUsagePercent,
            // Current Balance pake totalIncomeForCurrent
            currentBalance = formatRupiah(totalIncomeForCurrent - totalExpense),
            extraBalance = formatRupiah(extraBalance),
            isExtraPositive = extraBalance >= 0,
            graphData = graphData
        )
    }

    // 1. Toggle Listener
    fun toggleListener() {
        _uiState.update {
            it.copy(isListenerActive = !it.isListenerActive)
        }
    }

    // 1. Inject Current (Extra Balance DIAM)
    fun injectCurrentBalance(amount: Double) {
        viewModelScope.launch {
            val injection = TransactionEntity(
                amount = amount,
                note = "Manual Injection (Current)",
                category = "Injection_Current", // <--- KUNCI: Kategori Khusus
                isIncome = true,
                date = System.currentTimeMillis()
            )
            repository.insert(injection)
        }
    }

    // 2. Inject Extra (Current Balance DIAM)
    fun injectExtraBalance(amount: Double) {
        viewModelScope.launch {
            val injection = TransactionEntity(
                amount = amount,
                note = "Manual Injection (Extra)",
                category = "Injection_Extra", // <--- KUNCI: Kategori Khusus
                isIncome = true,
                date = System.currentTimeMillis()
            )
            repository.insert(injection)
        }
    }
}

private fun getCategoryStyle(category: String, isIncome: Boolean): Pair<Int, Color> {
    // 1. Warna Background (Bisa disesuaikan)
    val color = when (category) {
        "Food" -> Color(0xFFFF6D00)
        "Transport" -> Color(0xFF00C853)
        "Shopping" -> Color(0xFF7C4DFF)
        "Salary" -> Color(0xFF536DFE)
        else -> Color(0xFF90A4AE) // Warna Default
    }

    // 2. Icon Resource (R.drawable.xxx)
    val iconRes = if (isIncome) {
        R.drawable.ic_card_outline // Pastikan file ini ada
    } else {
        when (category) {
            "Food" -> R.drawable.ic_food_outline
            "Transport" -> R.drawable.ic_car_outline
            "Shopping" -> R.drawable.ic_cart_outline
            "Entertainment" -> R.drawable.ic_ticket_outline
            "Clothes" -> R.drawable.ic_bag_outline
            else -> R.drawable.ic_other_outline // Icon default (titik tiga/tanda tanya)
        }
    }

    return Pair(iconRes, color)
}

// Helper Class buat return banyak data sekaligus
data class AnalyticsState(
    val dailyLeft: String,
    val dailyUsagePercent: Float,
    val currentBalance: String,
    val extraBalance: String,
    val isExtraPositive: Boolean,
    val graphData: List<DayData>
)

// --- PABRIK VIEWMODEL (FACTORY) ---
// Ini wajib ada biar kita bisa nyuntik Repository ke ViewModel
class DashboardViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}