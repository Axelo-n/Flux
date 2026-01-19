package com.example.flux

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // 1. Simpan Transaksi (Otomatis background process pake suspend)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // 2. Hapus Transaksi
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // 3. Ambil Semua Data (Diurutkan dari yang terbaru)
    // Pake 'Flow' biar UI otomatis update real-time kalau ada data baru!
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // 4. Hitung Total Pemasukan
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    // 5. Hitung Total Pengeluaran
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    fun getTotalExpense(): Flow<Double?>
}