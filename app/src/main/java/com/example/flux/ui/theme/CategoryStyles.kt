package com.example.flux.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.flux.R

data class CategoryVisual(val name: String, val icon: Int, val color: Color)

/** Distinct category hues, tuned independently for each theme. */
private val categoryPalettes = mapOf(
    "teal" to listOf(0xFFE6C27A, 0xFFF1A783, 0xFFB6A5E4, 0xFFE69EB9, 0xFFDB9879, 0xFFAACB89, 0xFF8FB9E8, 0xFFE5A69B, 0xFF8ACEC8, 0xFFBFC780, 0xFFCEA2D9, 0xFF9DA6E0, 0xFFA6AFBB, 0xFF7FC5A4),
    "luca" to listOf(0xFFB17A00, 0xFFD46024, 0xFF7054C8, 0xFFD83F7C, 0xFFAD6840, 0xFF6B8C28, 0xFF277FBE, 0xFFC95044, 0xFF188F92, 0xFF9B7C15, 0xFF9F45B2, 0xFF4F63C7, 0xFF6D7584, 0xFF278655),
    "white" to listOf(0xFF9B751C, 0xFFBC6439, 0xFF725BA2, 0xFFB34E76, 0xFF986747, 0xFF66843B, 0xFF3C76A8, 0xFFB45E54, 0xFF367F83, 0xFF8A7B36, 0xFF935D9F, 0xFF586EAA, 0xFF6B7481, 0xFF3E815F),
    "black" to listOf(0xFFF2CC75, 0xFFFFAA78, 0xFFB99AF0, 0xFFF58FB2, 0xFFD8A082, 0xFFADD36E, 0xFF7DBCF1, 0xFFEB958D, 0xFF72D0CB, 0xFFCED579, 0xFFD48CE6, 0xFF91A6F2, 0xFFABB4C1, 0xFF73CE9D),
    "violet" to listOf(0xFFE4C188, 0xFFDEAA91, 0xFFAA9DE0, 0xFFE4A0BF, 0xFFC79781, 0xFFB5C88E, 0xFF9EBAE2, 0xFFDFA39F, 0xFF91C5C3, 0xFFCBC290, 0xFFD29EDC, 0xFF9DA8D8, 0xFFB5ACBD, 0xFF8CC5A8)
)
val categoryVisuals: List<CategoryVisual> get() = categoryVisuals(com.example.flux.preferences.AppPreferences.state.value.theme)
fun categoryVisuals(theme: String): List<CategoryVisual> {
    val tones = categoryPalettes.getValue(theme)
    return listOf(
        CategoryVisual("Breakfast", R.drawable.ic_category_breakfast, Color(tones[0])),
        CategoryVisual("Lunch", R.drawable.ic_category_lunch, Color(tones[1])),
        CategoryVisual("Dinner", R.drawable.ic_category_dinner, Color(tones[2])),
        CategoryVisual("Snack", R.drawable.ic_category_snack, Color(tones[3])),
        CategoryVisual("Extra food", R.drawable.ic_category_extra_food, Color(tones[4])),
        CategoryVisual("Food stock", R.drawable.ic_category_food_stock, Color(tones[5])),
        CategoryVisual("Drinks", R.drawable.ic_category_drinks, Color(tones[6])),
        CategoryVisual("Food and Beverages", R.drawable.ic_category_food, Color(tones[7])),
        CategoryVisual("Transportation", R.drawable.ic_category_transport, Color(tones[8])),
        CategoryVisual("Groceries and Shopping", R.drawable.ic_category_shopping, Color(tones[9])),
        CategoryVisual("Entertainment", R.drawable.ic_category_entertainment, Color(tones[10])),
        CategoryVisual("Account Transfer", R.drawable.ic_category_transfer, Color(tones[11])),
        CategoryVisual("Other", R.drawable.ic_category_other, Color(tones[12])),
        CategoryVisual("Income", R.drawable.ic_category_income, Color(tones[13]))
    )
}
fun categoryVisual(name: String): CategoryVisual = categoryVisuals.firstOrNull { it.name == name } ?: categoryVisuals.first { it.name == "Other" }.copy(name = name)
