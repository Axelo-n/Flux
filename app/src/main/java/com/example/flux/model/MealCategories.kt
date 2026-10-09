package com.example.flux.model

import java.time.Instant
import java.time.ZoneId

object MealCategories {
    val names = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Extra food", "Food stock", "Drinks")
    fun at(timestamp: Long, timezone: String = "Asia/Jakarta"): String {
        val hour = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of(timezone)).hour
        return when (hour) { in 5..9 -> "Breakfast"; in 10..14 -> "Lunch"; else -> "Dinner" }
    }
}
