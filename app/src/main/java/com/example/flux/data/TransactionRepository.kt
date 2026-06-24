package com.example.flux.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val parserRuleDao: ParserRuleDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()
    val allRules: Flow<List<ParserRule>> = parserRuleDao.getAllRules()

    suspend fun insert(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun delete(id: Int) = transactionDao.deleteById(id)
    suspend fun insertRule(rule: ParserRule) = parserRuleDao.insert(rule)
    suspend fun deleteRule(rule: ParserRule) = parserRuleDao.delete(rule)
    suspend fun getRulesSync(): List<ParserRule> = parserRuleDao.getAllRulesSync()
    suspend fun getAllTransactionsSync(): List<TransactionEntity> = transactionDao.getAllTransactionsSync()

    suspend fun restoreData(backup: FluxBackupData) {
        transactionDao.clearAll()
        parserRuleDao.clearAll()
        transactionDao.insertAll(backup.transactions)
        parserRuleDao.insertAll(backup.rules)
    }
}
