package com.example.flux.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.flux.R

data class CategoryVisual(val name: String, val icon: Int, val color: Color)

/** One icon family and a restrained, theme-aware palette for every category surface. */
val categoryVisuals: List<CategoryVisual> get() {
    val light = currentPalette.light
    val food = if (light) Color(0xFF9C5D28) else Color(0xFFE3B17C)
    val green = if (light) Color(0xFF34765C) else Color(0xFF8AC8AE)
    val blue = if (light) Color(0xFF3B6B94) else Color(0xFF94BAD9)
    val violet = if (light) Color(0xFF795D98) else Color(0xFFC0A6DB)
    val neutral = if (light) Color(0xFF647383) else Color(0xFFA5B3C2)
    return listOf(
        CategoryVisual("Breakfast", R.drawable.ic_category_breakfast, food),
        CategoryVisual("Lunch", R.drawable.ic_category_lunch, food),
        CategoryVisual("Dinner", R.drawable.ic_category_dinner, food),
        CategoryVisual("Snack", R.drawable.ic_category_snack, food),
        CategoryVisual("Extra food", R.drawable.ic_category_extra_food, food),
        CategoryVisual("Food stock", R.drawable.ic_category_food_stock, green),
        CategoryVisual("Drinks", R.drawable.ic_category_drinks, blue),
        CategoryVisual("Food and Beverages", R.drawable.ic_category_food, food),
        CategoryVisual("Transportation", R.drawable.ic_category_transport, blue),
        CategoryVisual("Groceries and Shopping", R.drawable.ic_category_shopping, green),
        CategoryVisual("Entertainment", R.drawable.ic_category_entertainment, violet),
        CategoryVisual("Account Transfer", R.drawable.ic_category_transfer, blue),
        CategoryVisual("Other", R.drawable.ic_category_other, neutral),
        CategoryVisual("Income", R.drawable.ic_category_income, green)
    )
}
fun categoryVisual(name: String): CategoryVisual = categoryVisuals.firstOrNull { it.name == name } ?: categoryVisuals.first { it.name == "Other" }.copy(name = name)
