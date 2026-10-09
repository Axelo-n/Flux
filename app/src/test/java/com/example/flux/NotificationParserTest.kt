package com.example.flux

import com.example.flux.data.ParserRule
import com.example.flux.notification.NotificationTransactionParser as Parser
import org.junit.Assert.*
import org.junit.Test

class NotificationParserTest {
    @Test fun mealHoursUseTheNotificationTimeAndCustomRulesWin() {
        fun at(hour: Int) = java.time.LocalDate.of(2026, 10, 9).atTime(hour, 0).atZone(java.time.ZoneId.of("Asia/Jakarta")).toInstant().toEpochMilli()
        val expected = mapOf(4 to "Dinner", 5 to "Breakfast", 9 to "Breakfast", 10 to "Lunch", 14 to "Lunch", 15 to "Dinner", 23 to "Dinner")
        expected.forEach { (hour, category) -> assertEquals(category, Parser.parse("Transaksi berhasil", "Transaksi di warung Rp 5000 berhasil", emptyList(), at(hour))!!.category) }
        val custom = ParserRule(keyword = "warung", targetCategory = "Food stock", targetNote = "Bekal")
        assertEquals("Food stock", Parser.parse("Transaksi berhasil", "Transaksi di warung Rp 5000 berhasil", listOf(custom), at(10))!!.category)
        assertEquals("Dinner", com.example.flux.model.MealCategories.at(at(10), "UTC"))
    }
    @Test fun cashbackIsAReturnNotAnIncomeOrNewExpense() {
        val cashback = Parser.parse("Cashback berhasil diterima", "Cashback Rp 2000 dari Warung Budi berhasil", emptyList())!!
        assertTrue(cashback.isCashback)
        assertFalse(cashback.isIncome)
        assertEquals(2000.0, cashback.amount, 0.0)
        assertEquals("Warung Budi", cashback.note)
        assertNull(Parser.parse("Promo cashback", "Dapatkan cashback Rp 2000", emptyList()))
        assertNull(Parser.parse("Cashback sedang diproses", "Rp 2000", emptyList()))
    }

    @Test fun bluOutgoingTransferWithAmountBeforeRecipientIsAnExpense() {
        val title = "Kamu Berhasil Mengirimkan Dana!"
        val text = "Transfer Rp 22.000 ke YOLANDA SETIAWAN berhasil"
        val parsed = Parser.parse(title, text, emptyList())!!
        assertEquals(22000.0, parsed.amount, 0.0)
        assertFalse(parsed.isIncome)
        assertEquals("Account Transfer", parsed.category)
        assertEquals("YOLANDA SETIAWAN", parsed.note)
        val rule = ParserRule(keyword = "yolanda setiawan", targetCategory = "Food and Beverages", targetNote = "Nasgor")
        val custom = Parser.parse(title, text, listOf(rule))!!
        assertEquals("Food and Beverages", custom.category)
        assertEquals("Nasgor", custom.note)
        assertFalse(custom.isIncome)
    }
    @Test fun outgoingTransferFormatStillRejectsFailedPendingAndBlacklistedNotifications() {
        val text = "Transfer Rp 22.000 ke YOLANDA SETIAWAN"
        listOf("gagal", "tidak berhasil", "sedang diproses", "akan diproses", "").forEach {
            assertNull(Parser.parse("blu", "$text $it", emptyList()))
        }
        assertNull(Parser.parse("blu", "$text berhasil", listOf(ParserRule(keyword = "yolanda", targetCategory = "Other", blocked = true))))
        assertNull(Parser.parse("blu", "$text berhasil, saldo Rp 900.000", emptyList()))
    }
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
