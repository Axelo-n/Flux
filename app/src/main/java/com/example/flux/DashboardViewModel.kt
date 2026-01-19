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
                    val (icon, color) = getCategoryStyle(entity.category, entity.isIncome)

                    Transaction(
                        id = entity.id,
                        title = entity.note.ifEmpty { entity.category }, // Kalo note kosong, pake nama kategori
                        category = entity.category,
                        amount = entity.amount,
                        formattedAmount = formatRupiah(entity.amount),
                        iconRes = icon,
                        iconBgColor = color,
                        isIncome = entity.isIncome
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