package com.example.flux

import androidx.compose.ui.graphics.Color

// 1. Model Data Transaksi (Dipindah kesini biar rapi)
data class Transaction(
    val id: Int,
    val title: String,
    val category: String,
    val amount: Double,
    val formattedAmount: String,

    // GANTI JADI INT (Karena R.drawable itu isinya Angka)
    val iconRes: Int,

    val iconBgColor: Color,
    val isIncome: Boolean,
    val date: Long
)

// 2. Model Data Grafik
data class DayData(
    val day: String,
    val amount: Float,
    val limit: Float
)

// 3. State Utama (Kumpulan semua data yang tampil di layar)
data class DashboardState(
    val isLoading: Boolean = true,
    val recentTransactions: List<Transaction> = emptyList(),
    val currentBalance: String = "Rp 0",
    val extraBalance: String = "Rp 0",
    val isExtraBalancePositive: Boolean = true,
    val dailyBudgetLeft: String = "Rp 0",

    // Pastikan dua ini ada:
    val dailyUsagePercent: Float = 0f,
    val graphData: List<DayData> = emptyList()
)