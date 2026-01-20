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
        var totalIncome = 0.0
        var totalExpense = 0.0
        var extraBalance = 0.0
        // (Opsional: Logic kurangi 320k tiap senin bisa ditaruh di backend/worker,
        // disini kita fokus ke kalkulasi transaksi berjalan dulu)

        // Grouping transaksi per hari (Key: "DayOfYear-Year")
        val txByDay = transactions.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
        }

        // 3. Loop Kalkulasi Extra Balance (Income - Overbudget)
        // Kita iterasi per hari yang ada transaksinya
        txByDay.forEach { (_, txList) ->
            // Ambil tanggal dari salah satu transaksi di grup ini buat cek hari apa
            val txDate = Calendar.getInstance().apply { timeInMillis = txList.first().date }
            val dayOfWeek = txDate.get(Calendar.DAY_OF_WEEK)

            // Logic Limit: Sabtu(7) & Minggu(1) = 60k, Lainnya = 40k
            val dailyLimit = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) 60000.0 else 40000.0

            val daysIncome = txList.filter { it.isIncome }.sumOf { it.amount }
            val daysExpense = txList.filter { !it.isIncome }.sumOf { it.amount }

            totalIncome += daysIncome
            totalExpense += daysExpense

            // Logic: Income masuk ke Extra Balance
            extraBalance += daysIncome

            // Logic: Cek Sisa Budget Harian
            val dailyLeft = dailyLimit - daysExpense

            // Logic: Kalo minus (tembus budget), potong Extra Balance
            if (dailyLeft < 0) {
                extraBalance += dailyLeft // dailyLeft negatif, jadi otomatis ngurangin
            }
        }

        // 4. Hitung Data Hari Ini (Untuk Card Pojok Kiri Atas)
        val dayOfWeekToday = calendar.get(Calendar.DAY_OF_WEEK)
        val limitToday = if (dayOfWeekToday == Calendar.SATURDAY || dayOfWeekToday == Calendar.SUNDAY) 60000.0 else 40000.0

        // Ambil transaksi hari ini doang
        val todayTx = transactions.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_YEAR) == todayDay && c.get(Calendar.YEAR) == todayYear
        }
        val expenseToday = todayTx.filter { !it.isIncome }.sumOf { it.amount }
        val dailyLeftToday = limitToday - expenseToday

        // 5. Hitung Weekly Percent (Untuk Battery Bar)
        // Asumsi total budget seminggu = 320.000 (40k*5 + 60k*2)
        // Kita hitung pengeluaran minggu ini saja
        val expenseThisWeek = 0.0 // (Implementasi filter minggu ini bisa ditambahkan disini)
        // Buat simpel, kita pake dummy logic persentase hari ini thd limit
        val dailyUsagePercent = (expenseToday / limitToday).toFloat().coerceIn(0f, 1f)


        // 6. Siapkan Data Graph (7 Hari Terakhir)
        val graphData = (0..6).map { i ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -((6 - i))) // Mundur dari H-6 sampai Hari H

            val dKey = "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
            val dOfWeek = c.get(Calendar.DAY_OF_WEEK)
            val dLimit = if (dOfWeek == Calendar.SATURDAY || dOfWeek == Calendar.SUNDAY) 60000f else 40000f

            // Cari total expense di hari tersebut
            val tList = txByDay[dKey] ?: emptyList()
            val dExpense = tList.filter { !it.isIncome }.sumOf { it.amount }.toFloat()

            // Format Label Hari (Mon, Tue)
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(c.time)

            DayData(dayLabel, dExpense, dLimit)
        }

        return AnalyticsState(
            dailyLeft = formatRupiah(dailyLeftToday),
            dailyUsagePercent = dailyUsagePercent,
            currentBalance = formatRupiah(totalIncome - totalExpense),
            extraBalance = formatRupiah(extraBalance), // Udah bersih (tanpa +)
            isExtraPositive = extraBalance >= 0,
            graphData = graphData
        )
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