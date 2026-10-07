package com.example.flux

import com.example.flux.data.ParserRule
import com.example.flux.notification.NotificationTransactionParser as Parser
import org.junit.Assert.*
import org.junit.Test

class NotificationParserTest {
    @Test fun merchantAndTransferDestinationBecomeNotes() {
        val expense = Parser.parse("blu", "Transaksi di gorengan gembleng Rp 5000 berhasil", emptyList())!!
        assertEquals("gorengan gembleng", expense.note)
        assertEquals(5000.0, expense.amount, 0.0)
        assertFalse(expense.isIncome)
        assertEquals("BUDI SANTOSO", Parser.parse("Transfer berhasil", "Transfer ke BUDI SANTOSO sebesar Rp 50.000 berhasil", emptyList())!!.note)
        assertEquals("Toko Rp Murah", Parser.parse("Pembayaran berhasil", "Pembayaran di Toko Rp Murah Rp10.000 berhasil", emptyList())!!.note)
    }
    @Test fun customNoteOverridesMerchantAndEmptyRuleNoteFallsBack() {
        val text = "Transaksi di gorengan gembleng Rp 5000 berhasil"
        val rule = ParserRule(keyword = "gembleng", targetCategory = "Food and Beverages", targetNote = "Nasgor")
        assertEquals("Nasgor", Parser.parse("blu", text, listOf(rule))!!.note)
        assertEquals("gorengan gembleng", Parser.parse("blu", text, listOf(rule.copy(targetNote = null)))!!.note)
        assertNull(Parser.parse("blu", text.replace("berhasil", "gagal"), emptyList()))
    }
    @Test fun blacklistWinsOverCustomCategory() {
        val rules = listOf(ParserRule(keyword = "seller", targetCategory = "Food", targetNote = "Nasgor"), ParserRule(keyword = "blugether", targetCategory = "", blocked = true))
        assertNull(Parser.parse("Pembayaran berhasil", "Rp 40.000 seller blugether", rules))
    }
    @Test fun customRuleSetsNoteAndDisabledRuleDoesNotApply() {
        val rule = ParserRule(keyword = "SELLER", targetCategory = "Food and Beverages", targetNote = "Nasgor")
        assertEquals("Nasgor", Parser.parse("Pembayaran berhasil", "Rp 40.000 seller", listOf(rule))?.note)
        assertEquals("", Parser.parse("Pembayaran berhasil", "Rp 40.000 seller", listOf(rule.copy(enabled = false)))?.note)
    }
    @Test fun incomeAndExpenseDetectedWithoutInventingAmounts() {
        assertTrue(Parser.parse("Dana masuk", "Rp50.000", emptyList())!!.isIncome)
        assertFalse(Parser.parse("Transfer berhasil", "Rp 50.000", emptyList())!!.isIncome)
        assertNull(Parser.parse("Transfer berhasil", "Nomor rekening 1234567890", emptyList()))
    }
    @Test fun failedPromotionalRefundAndAmbiguousMessagesNeedReview() {
        listOf("Pembayaran gagal", "Promo pembayaran", "Refund pembayaran", "Pembayaran sedang diproses").forEach { assertNull(Parser.parse(it, "Rp 50.000", emptyList())) }
        assertNull(Parser.parse("Pembayaran berhasil", "Rp 50.000 saldo Rp 900.000", emptyList()))
    }
    @Test fun validatesWholeRupiahAndFormatting() {
        assertEquals(1234567.0, Parser.extractAmount("Rp 1.234.567,00"), 0.0)
        assertEquals(50000.0, Parser.extractAmount("IDR 50000"), 0.0)
        assertEquals(0.0, Parser.extractAmount("Rp 50.000,50"), 0.0)
        assertEquals(0.0, Parser.extractAmount("Rp 12.34"), 0.0)
    }
    @Test fun acceptsNotificationPunctuationAndNonbreakingSpaces() {
        assertEquals(50000.0, Parser.extractAmount("Pembayaran Rp\u00a050.000. Terima kasih!"), 0.0)
        assertEquals(50000.0, Parser.extractAmount("Pembayaran Rp\u202f50.000,00."), 0.0)
        assertEquals(0.0, Parser.extractAmount("Pembayaran Rp 50.000,001"), 0.0)
    }
}
