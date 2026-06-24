package com.example.flux.notification

import com.example.flux.data.ParserRule
import java.util.Locale

data class ParsedTransaction(
    val amount: Double,
    val category: String,
    val note: String,
    val isIncome: Boolean
)

object NotificationTransactionParser {

    fun parse(title: String, text: String, customRules: List<ParserRule>): ParsedTransaction? {
        val fullText = "$title $text".lowercase(Locale.getDefault())

        val amount = extractAmount(fullText)
        if (amount <= 0) return null

        val isIncome = isIncomeTransaction(fullText)

        var category = "Other"
        var finalNote = if (text.length > 40) text.take(40) + "..." else text
        var ruleFound = false

        for (rule in customRules) {
            if (fullText.contains(rule.keyword.lowercase())) {
                category = rule.targetCategory
                if (!rule.targetNote.isNullOrEmpty()) finalNote = rule.targetNote
                ruleFound = true
                break
            }
        }

        if (!ruleFound) {
            category = detectCategory(fullText, isIncome)
        }

        return ParsedTransaction(amount = amount, category = category, note = finalNote, isIncome = isIncome)
    }

    private fun isIncomeTransaction(text: String): Boolean {
        return text.contains("dana masuk") ||
            text.contains("terima transfer") ||
            text.contains("transfer masuk") ||
            text.contains("refund")
    }

    fun extractAmount(text: String): Double {
        val lowerText = text.lowercase()

        val currencyRegex = Regex("(?:rp\\.?|idr)\\s*([\\d\\.]+)")
        val currencyMatch = currencyRegex.find(lowerText)
        if (currencyMatch != null) {
            return currencyMatch.groupValues[1].replace(".", "").toDoubleOrNull() ?: 0.0
        }

        val standaloneRegex = Regex("(?<![a-z0-9])(\\d[\\d\\.]+)(?![a-z0-9])")
        var maxAmount = 0.0
        for (match in standaloneRegex.findAll(lowerText)) {
            val num = match.value.replace(".", "").toDoubleOrNull() ?: 0.0
            if (num > maxAmount) maxAmount = num
        }
        return maxAmount
    }

    private fun detectCategory(text: String, isIncome: Boolean): String {
        if (isIncome) return "Income"

        val categories = mapOf(
            "Food and Beverages" to listOf("kopi", "makan", "food", "restoran", "cafe", "starbucks", "mcd", "kfc", "go-food", "gofood", "grabfood", "shopeefood"),
            "Transportation" to listOf("go-ride", "goride", "grab", "gojek", "bensin", "parkir", "tol", "shell", "pertamina"),
            "Groceries and Shopping" to listOf("indomaret", "alfamart", "superindo", "tokopedia", "shopee", "lazada", "tiktok"),
            "Entertainment" to listOf("netflix", "spotify", "steam", "bioskop", "cinema", "game", "top up game"),
            "Account Transfer" to listOf("transfer ke", "kirim dana", "bca", "mandiri", "bri", "bni")
        )

        for ((category, keywords) in categories) {
            if (keywords.any { text.contains(it) }) return category
        }

        return "Other"
    }
}
