package com.example.flux

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // 1. Simpan Transaksi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // 2. Hapus Transaksi
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // 3. Ambil Semua Data (Diurutkan dari yang terbaru)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // 4. Hitung Total Pemasukan
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    // 5. Hitung Total Pengeluaran
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    fun getTotalExpense(): Flow<Double?>

    // 6. Perintah Hapus berdasarkan ID
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    // BUAT BACKUP (Ambil semua tanpa Flow)
    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    // BUAT RESTORE (Masukin banyak sekaligus, timpa kalau id sama)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    // Hapus semua sebelum restore (Opsional, biar bersih)
    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}