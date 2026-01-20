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
            // Kita gabungin 3 sumber data: List Transaksi, Total Income, Total Expense
            combine(
                repository.allTransactions,
                repository.totalIncome,
                repository.totalExpense
            ) { transactions, income, expense ->
                Triple(transactions, income ?: 0.0, expense ?: 0.0)
            }.collect { (txList, totalIn, totalEx) ->

                // 1. Hitung Saldo
                val currentBalance = totalIn - totalEx
                val balanceFormatted = formatRupiah(currentBalance)
                val extraFormatted = formatRupiah(totalIn) // Semenatara pake total income buat extra

                // 2. Convert Entity (Database) ke UI Model (Tampilan)
                val uiTransactions = txList.map { entity ->
                    // Logic milih Icon & Warna berdasarkan Kategori
                    val (iconId, color) = getCategoryStyle(entity.category, entity.isIncome)

                    Transaction(
                        id = entity.id,
                        title = entity.note.ifEmpty { entity.category }, // Kalo note kosong, pake nama kategori
                        category = entity.category,
                        amount = entity.amount,
                        formattedAmount = formatRupiah(entity.amount),
                        iconRes = iconId,
                        iconBgColor = color,
                        isIncome = entity.isIncome,
                        date = entity.date
                    )
                }

                // 3. Logic Grafik Sederhana (Dummy logic biar grafik gerak dulu)
                // Nanti kita update biar real berdasarkan tanggal
                val graphData = _uiState.value.graphData // Pake data lama dulu

                // 4. Update UI State
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        currentBalance = balanceFormatted,
                        extraBalance = extraFormatted,
                        isExtraBalancePositive = true,
                        recentTransactions = uiTransactions, // Data list udah real!
                        dailyBudgetLeft = formatRupiah(60000.0 - (totalEx / 30)), // Simulasi sisa budget
                        graphData = graphData
                    )
                }
            }
        }
    }

    // 1. Fungsi Update Data ke Database
    fun updateTransaction(id: Int, amount: Double, note: String, category: String, isIncome: Boolean) {
        viewModelScope.launch {
            val updateTx = TransactionEntity(
                id = id, // ID lama wajib dibawa biar dia tau mana yang ditimpa
                amount = amount,
                note = note,
                category = category,
                isIncome = isIncome,
                // Kita pertahankan tanggal lama (kalau mau update tanggal jadi "sekarang", ganti jadi System.currentTimeMillis())
                date = System.currentTimeMillis()
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

    // Helper: Milih Icon & Warna
    private fun getCategoryStyle(category: String, isIncome: Boolean): Pair<Int, Color> {
        return if (isIncome) {
            Pair(R.drawable.ic_wallet_outline, CatBlue) // Default Income Icon
        } else {
            when (category) {
                "Food" -> Pair(R.drawable.ic_food_outline, CatOrange)
                "Transport" -> Pair(R.drawable.ic_cart_outline, CatPurple) // Ganti icon transport kalo ada
                "Shopping" -> Pair(R.drawable.ic_cart_outline, CatPurple)
                else -> Pair(R.drawable.ic_history_outline, UIGray)
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
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