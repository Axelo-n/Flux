package com.example.flux

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

        // 1. CARI ANGKA
        val amount = extractAmount(fullText)
        if (amount <= 0) return null

        // 2. TENTUKAN INCOME / EXPENSE
        val isIncome = isIncomeTransaction(fullText)

        // 3. CUSTOM RULES
        // Default value
        var category = "Other"
        var finalNote = if (text.length > 40) text.take(40) + "..." else text

        var ruleFound = false

        for (rule in customRules) {
            if (fullText.contains(rule.keyword.lowercase())) {
                category = rule.targetCategory

                if (!rule.targetNote.isNullOrEmpty()) {
                    finalNote = rule.targetNote
                }

                ruleFound = true
                break
            }
        }

        // 4. FALLBACK
        if (!ruleFound) {
            category = detectCategory(fullText, isIncome)
        }

        return ParsedTransaction(
            amount = amount,
            category = category,
            note = finalNote,
            isIncome = isIncome
        )
    }

    private fun isIncomeTransaction(text: String): Boolean {
        return text.contains("dana masuk") ||
                text.contains("terima transfer") ||
                text.contains("transfer masuk") ||
                text.contains("refund")
    }

    private fun extractAmount(text: String): Double {
        // Hapus titik ribuan
        var cleanText = text.replace(".", "").replace(",", "")

        var multiplier = 1.0
        if (cleanText.contains("juta")) {
            multiplier = 1000000.0
            cleanText = cleanText.replace("juta", "")
        } else if (cleanText.contains("ribu") || cleanText.contains("rb")) {
            multiplier = 1000.0
            cleanText = cleanText.replace("ribu", "").replace("rb", "")
        }

        val numberRegex = Regex("\\d+")
        val match = numberRegex.find(cleanText)

        return (match?.value?.toDoubleOrNull() ?: 0.0) * multiplier
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