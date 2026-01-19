package com.example.flux.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.DashboardState
import com.example.flux.DayData
import com.example.flux.R
import com.example.flux.Transaction
import com.example.flux.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    // State holder
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        // Otomatis load data pas app dibuka
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Simulasi loading
            delay(500)

            // 1. Siapkan Data Dummy Transaksi (Pindah dari UI ke sini)
            val dummyTransactions = listOf(
                Transaction(
                    id = 1,
                    title = "Transfer from Michael",
                    category = "Account Transfer",
                    amount = 150000.0,
                    formattedAmount = "Rp 150.000",
                    iconRes = R.drawable.ic_card_outline,
                    iconBgColor = CatBlue,
                    isIncome = true
                ),
                Transaction(
                    id = 2,
                    title = "Warung Mba Sri",
                    category = "Food and Beverages",
                    amount = 19000.0,
                    formattedAmount = "Rp 19.000",
                    iconRes = R.drawable.ic_food_outline,
                    iconBgColor = CatOrange,
                    isIncome = false
                ),
                Transaction(
                    id = 3,
                    title = "Aeon Supermarket",
                    category = "Groceries",
                    amount = 148300.0,
                    formattedAmount = "Rp 148.300",
                    iconRes = R.drawable.ic_cart_outline,
                    iconBgColor = CatPurple,
                    isIncome = false
                )
            )

            // 2. Siapkan Data Grafik
            val dummyGraph = listOf(
                DayData("Mon", 40f, 40f),
                DayData("Tue", 40f, 40f),
                DayData("Wed", 60f, 40f),
                DayData("Thu", 30f, 40f),
                DayData("Fri", 38f, 40f),
                DayData("Sat", 80f, 60f),
                DayData("Sun", 20f, 60f)
            )

            // 3. Masukkan ke State
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    currentBalance = "Rp 360.000",
                    extraBalance = "Rp 50.000",
                    isExtraBalancePositive = true,
                    dailyBudgetLeft = "Rp 60.000",
                    weeklyUsagePercent = 0.75f, // 75%
                    graphData = dummyGraph,
                    recentTransactions = dummyTransactions
                )
            }
        }
    }
}