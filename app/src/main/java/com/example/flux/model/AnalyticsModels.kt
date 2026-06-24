package com.example.flux.model

import androidx.compose.ui.graphics.Color

data class AnalyticsState(
    val dailyLeft: String,
    val dailyUsagePercent: Float,
    val currentBalance: String,
    val extraBalance: String,
    val isExtraPositive: Boolean,
    val graphData: List<DayData>
)

data class CategoryStat(
    val category: String,
    val total: Double,
    val percentage: Float,
    val color: Color,
    val icon: Int
)

data class MonthlyAnalyticsState(
    val totalExpense: String = "Rp 0",
    val totalIncome: String = "Rp 0",
    val categoryStats: List<CategoryStat> = emptyList(),
    val dailyGraphData: List<DayData> = emptyList()
)
