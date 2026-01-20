package com.example.flux

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.flux.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    // --- 1. STATE UTAMA (OTOMATIS / REAL-TIME) ---
    // Ini adalah "Jantung" dari ViewModel.
    // Dia menggabungkan data dari Database (Transactions, Income, Expense)
    // Lalu otomatis menghitung Analytics dan menghasilkan UI State baru.
    val uiState: StateFlow<DashboardState> = combine(
        repository.allTransactions,
        repository.totalIncome,
        repository.totalExpense
    ) { transactions, _, _ -> // Kita ignore income/expense raw dari repo, kita hitung manual di analytics biar akurat

        // A. Convert Entity Database ke Model UI
        val uiTransactions = transactions.map { entity ->
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

        // B. Hitung Analytics (Budget, Current Balance, Extra Balance)
        val analytics = calculateAnalytics(uiTransactions)

        // C. Return State Baru ke UI
        DashboardState(
            isLoading = false,
            recentTransactions = uiTransactions,
            dailyBudgetLeft = analytics.dailyLeft,
            dailyUsagePercent = analytics.dailyUsagePercent,
            currentBalance = analytics.currentBalance,
            extraBalance = analytics.extraBalance,
            isExtraBalancePositive = analytics.isExtraPositive,
            graphData = analytics.graphData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Tahan data 5 detik saat minimize
        initialValue = DashboardState() // State awal kosong
    )

    // --- 2. FUNGSI DATABASE (CRUD) ---

    fun addTransaction(amount: Double, note: String, category: String, isIncome: Boolean) {
        viewModelScope.launch {
            val newTx = TransactionEntity(
                amount = amount,
                note = note,
                category = category,
                isIncome = isIncome,
                date = System.currentTimeMillis()
            )
            repository.insert(newTx)
        }
    }

    fun updateTransaction(id: Int, amount: Double, note: String, category: String, isIncome: Boolean, date: Long) {
        viewModelScope.launch {
            val updateTx = TransactionEntity(
                id = id,
                amount = amount,
                note = note,
                category = category,
                isIncome = isIncome,
                date = date // Pertahankan tanggal asli
            )
            repository.insert(updateTx) // Room otomatis replace jika ID sama
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    // --- 3. FITUR DEBUG (INJECT BALANCE) ---

    fun injectCurrentBalance(amount: Double) {
        viewModelScope.launch {
            val injection = TransactionEntity(
                amount = amount,
                note = "Manual Injection (Current)",
                category = "Injection_Current", // Kategori Khusus
                isIncome = true,
                date = System.currentTimeMillis()
            )
            repository.insert(injection)
        }
    }

    fun injectExtraBalance(amount: Double) {
        viewModelScope.launch {
            val injection = TransactionEntity(
                amount = amount,
                note = "Manual Injection (Extra)",
                category = "Injection_Extra", // Kategori Khusus
                isIncome = true,
                date = System.currentTimeMillis()
            )
            repository.insert(injection)
        }
    }

    // --- 4. HELPER & LOGIC ---

    fun getTransactionById(id: Int): Transaction? {
        // Ambil dari state terakhir yang tersimpan
        return uiState.value.recentTransactions.find { it.id == id }
    }

    private fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    // Logic Icon & Warna (DISAMAKAN DENGAN ADD TRANSACTION SCREEN)
    private fun getCategoryStyle(category: String, isIncome: Boolean): Pair<Int, Color> {
        if (isIncome) {
            return Pair(R.drawable.ic_wallet_outline, CatBlue)
        }

        // Pastikan String ini SAMA PERSIS dengan yang ada di AddTransactionScreen.kt
        return when (category) {
            "Food and Beverages" -> Pair(R.drawable.ic_food_outline, CatOrange)
            "Transportation" -> Pair(R.drawable.ic_car_outline, CatGreen)
            "Groceries and Shopping" -> Pair(R.drawable.ic_cart_outline, CatPurple)
            "Entertainment" -> Pair(R.drawable.ic_ticket_outline, CatYellow)
            "Account Transfer" -> Pair(R.drawable.ic_card_outline, CatBlue)
            "Other" -> Pair(R.drawable.ic_other_outline, CatGrey)

            // Fallback buat kategori Injector atau yang aneh-aneh
            "Injection_Current" -> Pair(R.drawable.ic_wallet_outline, UITeal)
            "Injection_Extra" -> Pair(R.drawable.ic_wallet_outline, UIBlue)
            else -> Pair(R.drawable.ic_other_outline, CatGrey)
        }
    }

    // Logic Perhitungan Keuangan (Separation Current vs Extra)
    fun calculateAnalytics(transactions: List<Transaction>): AnalyticsState {
        val calendar = Calendar.getInstance()
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        var totalIncomeForCurrent = 0.0
        var totalExpense = 0.0
        var extraBalance = 0.0

        val txByDay = transactions.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
        }

        // Loop Hari
        txByDay.forEach { (_, txList) ->
            val txDate = Calendar.getInstance().apply { timeInMillis = txList.first().date }
            val dayOfWeek = txDate.get(Calendar.DAY_OF_WEEK)
            val dailyLimit = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) 60000.0 else 40000.0

            // Filter Income biar ga saling ganggu
            val incomeForCurrent = txList.filter { it.isIncome && it.category != "Injection_Extra" }.sumOf { it.amount }
            val incomeForExtra = txList.filter { it.isIncome && it.category != "Injection_Current" }.sumOf { it.amount }

            // Filter Expense (Exclude Injection Categories just in case)
            val daysExpense = txList.filter { !it.isIncome }.sumOf { it.amount }

            totalIncomeForCurrent += incomeForCurrent
            totalExpense += daysExpense
            extraBalance += incomeForExtra

            val dailyLeft = dailyLimit - daysExpense
            if (dailyLeft < 0) {
                extraBalance += dailyLeft // Kurangi extra balance kalau overbudget
            }
        }

        // Data Hari Ini
        val dayOfWeekToday = calendar.get(Calendar.DAY_OF_WEEK)
        val limitToday = if (dayOfWeekToday == Calendar.SATURDAY || dayOfWeekToday == Calendar.SUNDAY) 60000.0 else 40000.0
        val todayTx = transactions.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_YEAR) == todayDay && c.get(Calendar.YEAR) == todayYear
        }
        val expenseToday = todayTx.filter { !it.isIncome }.sumOf { it.amount }
        val dailyLeftToday = limitToday - expenseToday
        val dailyUsagePercent = (expenseToday / limitToday).toFloat().coerceIn(0f, 1f)

        // Data Grafik
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
            currentBalance = formatRupiah(totalIncomeForCurrent - totalExpense),
            extraBalance = formatRupiah(extraBalance),
            isExtraPositive = extraBalance >= 0,
            graphData = graphData
        )
    }
}

// Class Pembantu buat Data Analytics
data class AnalyticsState(
    val dailyLeft: String,
    val dailyUsagePercent: Float,
    val currentBalance: String,
    val extraBalance: String,
    val isExtraPositive: Boolean,
    val graphData: List<DayData>
)

// Factory Boilerplate
class DashboardViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}