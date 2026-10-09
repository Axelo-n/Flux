package com.example.flux.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_config")
data class FinanceConfig(
    @PrimaryKey val id: Int = 1,
    val startDay: Long,
    val timezone: String = "Asia/Jakarta",
    val openingBalance: Long,
    val activatedAt: Long = System.currentTimeMillis()
)

/** Monday through Sunday, in whole rupiah. Policies are effective-dated, never overwritten. */
@Entity(tableName = "budget_policies")
data class BudgetPolicy(
    @PrimaryKey val effectiveDay: Long,
    val amounts: String
) {
    fun weeklyAmounts(): List<Long> = amounts.split(",").map { it.toLong() }.also {
        require(it.size == 7 && it.all { amount -> amount >= 0 })
    }
}

@Entity(tableName = "balance_adjustments")
data class BalanceAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val target: String,
    val amount: Long,
    val note: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_records")
data class NotificationRecord(
    @PrimaryKey val eventId: String,
    val fingerprint: String,
    val postedAt: Long,
    val title: String,
    val text: String,
    val status: String,
    val detail: String,
    val occurrenceId: String = eventId
)

/** A planning allocation, never an expense or balance adjustment. */
@Entity(tableName = "monthly_pockets")
data class MonthlyPocket(@PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String, val amount: Long)
