package com.example.flux.model

import androidx.compose.ui.graphics.Color

data class Transaction(
    val id: Int,
    val title: String,
    val category: String,
    val amount: Double,
    val formattedAmount: String,
    val iconRes: Int,
    val iconBgColor: Color,
    val isIncome: Boolean,
    val date: Long
)

data class DayData(
    val day: String,
    val amount: Float,
    val limit: Float
)
