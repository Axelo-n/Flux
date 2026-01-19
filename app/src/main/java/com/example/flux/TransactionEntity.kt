package com.example.flux

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions") // Nama tabel di SQL
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ID otomatis nambah (1, 2, 3...)

    val amount: Double,
    val note: String,
    val category: String,
    val isIncome: Boolean,
    val date: Long = System.currentTimeMillis() // Simpan waktu pas dibuat
)