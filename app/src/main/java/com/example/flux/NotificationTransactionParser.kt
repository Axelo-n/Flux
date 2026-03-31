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

    fun extractAmount(text: String): Double {
        val lowerText = text.lowercase()

        // --- PRIORITAS 1: Cari Format Currency (Rp / IDR) ---
        // Penjelasan Regex:
        // (?:rp\.?|idr) -> Cari kata "rp", "rp.", atau "idr" (hiraukan case)
        // \s*           -> Spasi boleh ada, boleh nggak
        // ([\d\.]+)     -> Tangkap semua angka dan titik setelahnya
        val currencyRegex = Regex("(?:rp\\.?|idr)\\s*([\\d\\.]+)")
        val currencyMatch = currencyRegex.find(lowerText)

        if (currencyMatch != null) {
            // Ambil grup ke-1 (angkanya aja), lalu buang titik ribuan
            val rawNumber = currencyMatch.groupValues[1].replace(".", "")
            return rawNumber.toDoubleOrNull() ?: 0.0
        }

        // --- PRIORITAS 2: Cari Angka Standalone (Berdiri Sendiri) ---
        // Penjelasan Regex:
        // (?<![a-z0-9]) -> Sebelum angka, TIDAK BOLEH ada huruf atau angka lain
        // (\d[\d\.]+)   -> Tangkap angka utamanya (minimal 2 digit/titik biar ga nangkep typo)
        // (?![a-z0-9])  -> Setelah angka, TIDAK BOLEH ada huruf atau angka lain
        val standaloneRegex = Regex("(?<![a-z0-9])(\\d[\\d\\.]+)(?![a-z0-9])")
        val matches = standaloneRegex.findAll(lowerText)

        var maxAmount = 0.0
        for (match in matches) {
            val rawNumber = match.value.replace(".", "")
            val num = rawNumber.toDoubleOrNull() ?: 0.0

            // Kita ambil angka terbesar yang ditemuin di teks
            if (num > maxAmount) {
                maxAmount = num
            }
        }

        return maxAmount
    }
//    private fun extractAmount(text: String): Double {
//        // Hapus titik ribuan
//        var cleanText = text.replace(".", "").replace(",", "")
//
//        var multiplier = 1.0
//        if (cleanText.contains("juta")) {
//            multiplier = 1000000.0
//            cleanText = cleanText.replace("juta", "")
//        } else if (cleanText.contains("ribu") || cleanText.contains("rb")) {
//            multiplier = 1000.0
//            cleanText = cleanText.replace("ribu", "").replace("rb", "")
//        }
//
//        val numberRegex = Regex("\\d+")
//        val match = numberRegex.find(cleanText)
//
//        return (match?.value?.toDoubleOrNull() ?: 0.0) * multiplier
//    }

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