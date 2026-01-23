package com.example.flux

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
    val date: Long = System.currentTimeMillis()
)