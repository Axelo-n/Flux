package com.example.flux

import androidx.compose.ui.graphics.Color

// 1. Model Data Transaksi (Dipindah kesini biar rapi)
data class Transaction(
    val id: Int,
    val title: String,
    val category: String,
    val amount: Double,           // Buat hitungan logika
    val formattedAmount: String,  // Buat tampilan teks (Rp ...)
    val iconRes: Int,
    val iconBgColor: Color,
    val isIncome: Boolean
)

// 2. Model Data Grafik
data class DayData(
    val day: String,
    val amount: Float,
    val limit: Float
)

// 3. State Utama (Kumpulan semua data yang tampil di layar)
data class DashboardState(
    val isLoading: Boolean = false,
    val isListenerActive: Boolean = true,

    // Header Data
    val currentBalance: String = "Rp 0",
    val extraBalance: String = "Rp 0",
    val isExtraBalancePositive: Boolean = true,

    // Budget Data
    val dailyBudgetLeft: String = "Rp 0",
    val weeklyUsagePercent: Float = 0.0f, // 0.0 sampai 1.0

    // List Data
    val graphData: List<DayData> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList()
)