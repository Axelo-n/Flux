package com.example.flux.model

data class DashboardState(
    val isLoading: Boolean = true,
    val recentTransactions: List<Transaction> = emptyList(),
    val currentBalance: String = "Rp 0",
    val extraBalance: String = "Rp 0",
    val isExtraBalancePositive: Boolean = true,
    val dailyBudgetLeft: String = "Rp 0",
    val dailyUsagePercent: Float = 0f,
    val graphData: List<DayData> = emptyList(),
    val totals: BudgetTotals = BudgetTotals(),
    val configured: Boolean = false
)
