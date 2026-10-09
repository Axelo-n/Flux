package com.example.flux

import com.example.flux.model.PocketPlanner
import com.example.flux.data.BudgetPolicy
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class PocketPlannerTest {
    @Test fun clipsMondaySundayWeeksAndIncludesEveryDateExactlyOnce() {
        val month = YearMonth.of(2026, 4) // Wednesday start, Thursday end.
        val policy = BudgetPolicy(month.atDay(1).toEpochDay(), "10000,20000,30000,40000,50000,60000,70000")
        val weeks = PocketPlanner.plan(month, listOf(policy))
        assertEquals(listOf(5, 7, 7, 7, 4), weeks.map { it.days.size })
        assertEquals(250000L, weeks.first().total)
        val days = weeks.flatMap { it.days }
        assertEquals(30, days.size); assertEquals(30, days.distinct().size)
        assertEquals(month.atDay(1), days.first()); assertEquals(month.atEndOfMonth(), days.last())
    }
    @Test fun honorsScheduledPoliciesAndUsesEarliestPatternBeforeSetup() {
        val month = YearMonth.of(2026, 2)
        val policies = listOf(BudgetPolicy(month.atDay(5).toEpochDay(), List(7) { 50000 }.joinToString(",")), BudgetPolicy(month.atDay(15).toEpochDay(), List(7) { 60000 }.joinToString(",")))
        val weeks = PocketPlanner.plan(month, policies)
        assertEquals(1, weeks.first().days.size)
        assertEquals(14 * 50000L + 14 * 60000L, weeks.sumOf { it.total })
        assertEquals(29, PocketPlanner.plan(YearMonth.of(2028, 2), policies).sumOf { it.days.size })
        assertEquals(0L, PocketPlanner.plan(month, emptyList()).sumOf { it.total })
    }
}
