package com.example.flux.model

import com.example.flux.data.BudgetPolicy
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

data class PocketWeek(val number: Int, val days: List<LocalDate>, val amounts: List<Long>) {
    val total: Long get() = amounts.sum()
}

object PocketPlanner {
    fun plan(month: YearMonth, policies: List<BudgetPolicy>): List<PocketWeek> {
        val result = mutableListOf<PocketWeek>()
        var start = month.atDay(1)
        val last = month.atEndOfMonth()
        val earliest = policies.minByOrNull { it.effectiveDay }
        while (start <= last) {
            val end = minOf(start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), last)
            val days = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()
            val amounts = days.map { day ->
                if (earliest != null && day.toEpochDay() < earliest.effectiveDay) earliest.weeklyAmounts()[day.dayOfWeek.value - 1]
                else BudgetEngine.budget(day, policies)
            }
            result += PocketWeek(result.size + 1, days, amounts)
            start = end.plusDays(1)
        }
        return result
    }
}
