package com.example.flux

import com.example.flux.data.*
import com.example.flux.model.BudgetEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BudgetEngineTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private val config = FinanceConfig(startDay = monday.toEpochDay(), openingBalance = 1_000_000)
    private val policy = BudgetPolicy(monday.toEpochDay(), List(7) { 50_000 }.joinToString(","))
    private fun tx(day: LocalDate, amount: Double, income: Boolean = false, refund: Double = 0.0) = TransactionEntity(amount = amount, note = "", category = "Other", isIncome = income, date = day.atTime(12, 0).atZone(ZoneId.of("Asia/Jakarta")).toInstant().toEpochMilli(), refund = refund)
    @Test fun firstDayGetsFullBudgetAndNoExtra() {
        val result = BudgetEngine.calculate(config, listOf(policy), emptyList(), emptyList(), monday)
        assertEquals(50_000L, result.remaining); assertEquals(0L, result.extra); assertEquals(1_000_000L, result.balance)
    }
    @Test fun savingsThenOverspendLeaveTomorrowIntact() {
        val rows = listOf(tx(monday, 40_000.0), tx(monday.plusDays(1), 55_000.0))
        val tuesday = BudgetEngine.calculate(config, listOf(policy), rows, emptyList(), monday.plusDays(1))
        assertEquals(5_000L, tuesday.extra); assertEquals(5_000L, tuesday.available)
        val wednesday = BudgetEngine.calculate(config, listOf(policy), rows, emptyList(), monday.plusDays(2))
        assertEquals(50_000L, wednesday.remaining); assertEquals(5_000L, wednesday.extra)
    }
    @Test fun incomeOnlyChangesAccountBalance() {
        val result = BudgetEngine.calculate(config, listOf(policy), listOf(tx(monday, 200_000.0, true)), emptyList(), monday)
        assertEquals(1_200_000L, result.balance); assertEquals(0L, result.extra)
    }
    @Test fun futureBudgetDoesNotRepricePastDays() {
        val next = BudgetPolicy(monday.plusDays(1).toEpochDay(), List(7) { 75_000 }.joinToString(","))
        val result = BudgetEngine.calculate(config, listOf(next, policy), listOf(tx(monday, 40_000.0)), emptyList(), monday.plusDays(1))
        assertEquals(10_000L, result.extra); assertEquals(75_000L, result.budget)
    }
    @Test fun refundRecomputesOriginalDayAndLeavesZeroRecord() {
        val result = BudgetEngine.calculate(config, listOf(policy), listOf(tx(monday, 100_000.0, refund = 100_000.0)), emptyList(), monday.plusDays(1))
        assertEquals(1_000_000L, result.balance); assertEquals(50_000L, result.extra)
        val partial = BudgetEngine.calculate(config, listOf(policy), listOf(tx(monday, 100_000.0, refund = 30_000.0)), emptyList(), monday.plusDays(1))
        assertEquals(930_000L, partial.balance); assertEquals(-20_000L, partial.extra)
    }
    @Test fun correctionsHaveIndependentTargets() {
        val date = tx(monday, 1.0).date
        val adjustments = listOf(BalanceAdjustment(target = "saldo", amount = -20_000, note = "fix", date = date), BalanceAdjustment(target = "extra", amount = 10_000, note = "fix", date = date))
        val result = BudgetEngine.calculate(config, listOf(policy), emptyList(), adjustments, monday)
        assertEquals(980_000L, result.balance); assertEquals(10_000L, result.extra)
    }
    @Test fun dayBoundaryUsesConfiguredZone() {
        val instant = Instant.parse("2026-10-05T17:00:00Z").toEpochMilli()
        assertEquals(monday.plusDays(1), BudgetEngine.day(instant, "Asia/Jakarta"))
    }
    @Test fun noSpendingAccumulatesButNeverBeforeStart() {
        assertEquals(100_000L, BudgetEngine.calculate(config, listOf(policy), emptyList(), emptyList(), monday.plusDays(2)).extra)
        assertEquals(0L, BudgetEngine.calculate(config, listOf(policy), emptyList(), emptyList(), monday.minusDays(1)).budget)
    }
    @Test fun zeroBudgetAndNegativeExtraDoNotDivideOrDoubleDebit() {
        val zero = BudgetPolicy(monday.toEpochDay(), List(7) { 0 }.joinToString(","))
        val result = BudgetEngine.calculate(config, listOf(zero), listOf(tx(monday, 10_000.0)), emptyList(), monday)
        assertEquals(-10_000L, result.extra); assertEquals(-10_000L, result.available)
    }
    @Test fun spendingRoomCannotExceedRecordedAccountFunds() {
        val limited = config.copy(openingBalance = 20_000)
        val result = BudgetEngine.calculate(limited, listOf(policy), emptyList(), emptyList(), monday.plusDays(2))
        assertEquals(100_000L, result.extra)
        assertEquals(20_000L, result.available)
    }
}
