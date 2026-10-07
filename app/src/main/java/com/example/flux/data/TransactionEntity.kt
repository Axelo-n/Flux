package com.example.flux.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val note: String,
    val category: String,
    val isIncome: Boolean,
    val date: Long = System.currentTimeMillis(),
    val refund: Double = 0.0,
    val refundNote: String = "",
    val source: String = "manual"
)

data class FluxBackupData(
    val transactions: List<TransactionEntity>,
    val rules: List<ParserRule>,
    val version: Int = 2,
    val config: FinanceConfig? = null,
    val policies: List<BudgetPolicy> = emptyList(),
    val adjustments: List<BalanceAdjustment> = emptyList()
)
