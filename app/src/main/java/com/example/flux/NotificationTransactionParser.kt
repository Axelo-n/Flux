package com.example.flux

import java.util.Locale

data class ParsedTransaction(
    val amount: Double,
    val category: String,
    val note: String,
    val isIncome: Boolean
)

object NotificationTransactionParser {

    fun parse(title: String, text: String): ParsedTransaction? {
        val fullText = "$title $text".lowercase(Locale.getDefault())

        // 1. CARI ANGKA
        val amount = extractAmount(fullText)
        if (amount <= 0) return null

        // 2. TENTUKAN INCOME / EXPENSE (Logic Khusus Bank)
        val isIncome = isIncomeTransaction(fullText)

        // 3. TEBAK KATEGORI
        val category = detectCategory(fullText, isIncome)

        // 4. BERSIHKAN NOTE
        val note = if (text.length > 40) text.take(40) + "..." else text

        return ParsedTransaction(
            amount = amount,
            category = category,
            note = note,
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
        if (isIncome) return "Salary"

        // LOGIC KATEGORI
        return when {
            // Makanan
            text.contains("kopi") || text.contains("cafe") || text.contains("resto") ||
                    text.contains("food") || text.contains("mcd") || text.contains("kfc") -> "Food and Beverages"

            // Transport & Bensin
            text.contains("gojek") || text.contains("grab") || text.contains("shell") ||
                    text.contains("pertamina") || text.contains("parkir") -> "Transportation"

            // Belanja
            text.contains("tokopedia") || text.contains("shopee") || text.contains("alfamart") ||
                    text.contains("indomaret") || text.contains("supermarket") -> "Groceries and Shopping"

            // Hiburan/Langganan
            text.contains("netflix") || text.contains("spotify") || text.contains("steam") ||
                    text.contains("google play") -> "Entertainment"

            // Transfer ke orang / Topup E-wallet
            text.contains("transfer") || text.contains("top up") || text.contains("gopay") ||
                    text.contains("ovo") || text.contains("dana") -> "Account Transfer"

            else -> "Other"
        }
    }
}