package com.example.flux

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    // Ambil semua data (Live update)
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    // Hitung total (Live update)
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()

    // Simpan data
    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun delete(id: Int) {
        transactionDao.deleteById(id)
    }
}