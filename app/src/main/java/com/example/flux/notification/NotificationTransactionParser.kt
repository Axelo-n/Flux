package com.example.flux.notification

import com.example.flux.data.ParserRule
import com.example.flux.model.BudgetEngine
import java.util.Locale

data class ParsedTransaction(val amount: Double, val category: String, val note: String, val isIncome: Boolean)

object NotificationTransactionParser {
    private fun full(title: String, text: String) = "$title $text".lowercase(Locale.ROOT)
    fun blockedBy(title: String, text: String, rules: List<ParserRule>): ParserRule? {
        val content = full(title, text)
        return rules.firstOrNull { it.enabled && it.blocked && it.keyword.isNotBlank() && content.contains(it.keyword.trim().lowercase(Locale.ROOT)) }
    }
    fun parse(title: String, text: String, customRules: List<ParserRule>): ParsedTransaction? {
        if (blockedBy(title, text, customRules) != null) return null
        val content = full(title, text)
        // Refund requires linking to an original expense. Never silently turn it into income.
        if (listOf("refund", "pengembalian dana", "dikembalikan", "dibatalkan").any { content.contains(it) }) return null
        if (listOf("gagal", "tidak berhasil", "otp", "kode verifikasi", "akan diproses", "sedang diproses", "promo").any { content.contains(it) }) return null
        val income = listOf("dana masuk", "terima transfer", "transfer masuk", "menerima transfer", "transfer diterima").any { content.contains(it) }
        // blu places the amount between "Transfer" and "ke", so literal "transfer ke" misses it.
        val outgoingTransfer = Regex("\\btransfer\\s+(?:rp\\.?|idr)\\s*[0-9].*?\\b(?:ke|kepada)\\s+.+?\\s+(?:berhasil|sukses)[.!]?\\s*$", RegexOption.DOT_MATCHES_ALL).containsMatchIn(content)
        val expense = outgoingTransfer || listOf("pembayaran", "bayar", "transfer ke", "transfer berhasil", "kirim dana", "transfer keluar", "tarik tunai", "top up", "topup", "transaksi berhasil", "pembelian", "qris berhasil").any { content.contains(it) } || (Regex("\\btransaksi\\s+di\\b").containsMatchIn(content) && content.contains("berhasil"))
        if (!income && !expense) return null
        val amount = extractAmount(content)
        if (amount <= 0 || amount > BudgetEngine.MAX_AMOUNT) return null
        val rule = customRules.firstOrNull { it.enabled && !it.blocked && it.keyword.isNotBlank() && content.contains(it.keyword.trim().lowercase(Locale.ROOT)) }
        return ParsedTransaction(amount, rule?.targetCategory ?: category(content, income), rule?.targetNote?.takeIf { it.isNotBlank() } ?: extractCounterparty(text).ifBlank { extractCounterparty(title) }, income)
    }
    private fun extractCounterparty(text: String): String {
        // Preserve the seller's spelling; currency, status and account numbers are metadata.
        val patterns = listOf(
            "(?:tujuan|penerima|merchant|atas nama)\\s*:?\\s+(.+)",
            "(?:transaksi|pembayaran|pembelian|bayar)(?:\\s+(?:berhasil|sukses))?\\s+(?:di|ke|kepada|untuk)\\s+(.+)",
            "(?:transfer|kirim dana).*?\\b(?:ke|kepada)\\s+(.+)",
            "(?:dana masuk|terima transfer|menerima transfer|transfer masuk|transfer diterima).*?\\bdari\\s+(.+)",
            "(?:tujuan|penerima|merchant)\\s*:\\s*(.+)"
        )
        val match = patterns.firstNotNullOfOrNull { Regex(it, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) } ?: return ""
        return match.split(Regex("\\s+(?:sebesar|senilai|sejumlah|dengan nominal|berhasil|sukses|telah|melalui|pada|nomor rekening|no\\.? rekening|rekening)\\b|(?:rp\\.?|idr)\\s*[0-9]|[\\n\\r]", RegexOption.IGNORE_CASE)).first().trim(' ', '.', ',', ':', '-', '"', '\'').take(200).takeUnless { it.isBlank() || it.all { c -> c.isDigit() || c.isWhitespace() || c == '*' } }.orEmpty()
    }
    fun extractAmount(text: String): Double {
        val matches = Regex("(?:rp\\.?|idr)[\\s\\u00a0\\u202f]*([0-9]+(?:\\.[0-9]{3})*)(?:,([0-9]{2}))?(?![0-9]|[.,][0-9])", RegexOption.IGNORE_CASE).findAll(text).toList()
        // Multiple currency values (e.g. amount + remaining balance) need manual review.
        if (matches.size != 1) return 0.0
        val match = matches.single()
        if (match.groupValues[2].isNotEmpty() && match.groupValues[2] != "00") return 0.0
        return match.groupValues[1].replace(".", "").toDoubleOrNull() ?: 0.0
    }
    private fun category(text: String, income: Boolean): String {
        if (income) return "Income"
        val categories = linkedMapOf(
            "Food and Beverages" to listOf("kopi", "makan", "food", "restoran", "cafe", "starbucks", "mcd", "kfc"),
            "Transportation" to listOf("go-ride", "goride", "grab", "gojek", "bensin", "parkir", "tol", "shell", "pertamina"),
            "Groceries and Shopping" to listOf("indomaret", "alfamart", "superindo", "tokopedia", "shopee", "lazada", "tiktok"),
            "Entertainment" to listOf("netflix", "spotify", "steam", "bioskop", "cinema", "game"),
            "Account Transfer" to listOf("transfer", "kirim dana")
        )
        return categories.entries.firstOrNull { (_, words) -> words.any { text.contains(it) } }?.key ?: "Other"
    }
}
