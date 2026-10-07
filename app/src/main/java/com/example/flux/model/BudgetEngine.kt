package com.example.flux.model

import com.example.flux.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BudgetTotals(val balance: Long = 0, val budget: Long = 0, val spent: Long = 0, val remaining: Long = 0, val extra: Long = 0, val graph: List<DayData> = emptyList()) {
    // Overspending already debits extra; do not subtract negative remaining twice.
    val available: Long get() = (remaining.coerceAtLeast(0) + extra).coerceAtMost(balance)
}

object BudgetEngine {
    const val MAX_AMOUNT = 9_000_000_000_000L
    fun day(timestamp: Long, zone: String): LocalDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of(zone)).toLocalDate()
    fun budget(date: LocalDate, policies: List<BudgetPolicy>): Long = policies.filter { it.effectiveDay <= date.toEpochDay() }.maxByOrNull { it.effectiveDay }?.weeklyAmounts()?.get(date.dayOfWeek.value - 1) ?: 0
    fun calculate(config: FinanceConfig?, policies: List<BudgetPolicy>, transactions: List<TransactionEntity>, adjustments: List<BalanceAdjustment>, today: LocalDate): BudgetTotals {
        if (config == null) return BudgetTotals()
        val start = LocalDate.ofEpochDay(config.startDay)
        val valid = transactions.filter { day(it.date, config.timezone) in start..today }
        val corrections = adjustments.filter { day(it.date, config.timezone) in start..today }
        val expenses = valid.filter { !it.isIncome }.groupBy { day(it.date, config.timezone) }.mapValues { (_, rows) -> rows.sumOf { (it.amount - it.refund).toLong() } }
        val balance = config.openingBalance + valid.sumOf { if (it.isIncome) it.amount.toLong() else -(it.amount - it.refund).toLong() } + corrections.filter { it.target == "saldo" }.sumOf { it.amount }
        var extra = corrections.filter { it.target == "extra" }.sumOf { it.amount }
        var date = start
        while (date < today) {
            extra += budget(date, policies) - (expenses[date] ?: 0)
            date = date.plusDays(1)
        }
        val limit = if (today >= start) budget(today, policies) else 0
        val spent = expenses[today] ?: 0
        val remaining = limit - spent
        extra += remaining.coerceAtMost(0)
        val graph = (6 downTo 0).map { ago ->
            val d = today.minusDays(ago.toLong())
            DayData(d.dayOfWeek.name.take(3), (expenses[d] ?: 0).toFloat(), if (d >= start) budget(d, policies).toFloat() else 0f)
        }
        return BudgetTotals(balance, limit, spent, remaining, extra, graph)
    }
}
