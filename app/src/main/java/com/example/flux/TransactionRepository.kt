package com.example.flux

import kotlinx.coroutines.flow.Flow

// Update Constructor: Tambahkan parserRuleDao
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val parserRuleDao: ParserRuleDao
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()

    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun delete(id: Int) {
        transactionDao.deleteById(id)
    }

    // --- CUSTOM RULES ---

    // 1. Live Data
    val allRules: Flow<List<ParserRule>> = parserRuleDao.getAllRules()

    // 2. Add/Delete
    suspend fun insertRule(rule: ParserRule) {
        parserRuleDao.insert(rule)
    }

    suspend fun deleteRule(rule: ParserRule) {
        parserRuleDao.delete(rule)
    }

    // 3. Service
    suspend fun getRulesSync(): List<ParserRule> {
        return parserRuleDao.getAllRulesSync()
    }
}