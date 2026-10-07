package com.example.flux.data

import androidx.room.withTransaction
import com.example.flux.model.BudgetEngine
import com.example.flux.notification.NotificationTransactionParser
import java.time.LocalDate
import java.time.ZoneId

class TransactionRepository(private val database: TransactionDatabase) {
    private val transactions = database.transactionDao()
    private val rules = database.parserRuleDao()
    private val finance = database.financeDao()
    val allTransactions = transactions.getAllTransactions()
    val allRules = rules.getAllRules()
    val config = finance.observeConfig()
    val policies = finance.observePolicies()
    val adjustments = finance.observeAdjustments()
    val notifications = finance.observeNotifications()
    suspend fun insert(transaction: TransactionEntity) {
        validateTransaction(transaction)
        val settings = requireNotNull(finance.config()) { "Atur sistem dulu" }
        val date = BudgetEngine.day(transaction.date, settings.timezone)
        require(date >= LocalDate.ofEpochDay(settings.startDay) && date <= LocalDate.now(ZoneId.of(settings.timezone))) { "Tanggal di luar periode pencatatan" }
        transactions.insertTransaction(transaction)
    }
    suspend fun delete(id: Int) = transactions.deleteById(id)
    suspend fun insertRule(rule: ParserRule) {
        require(rule.keyword.isNotBlank()) { "Keyword wajib diisi" }
        rules.insert(rule.copy(keyword = rule.keyword.trim()))
    }
    suspend fun updateRule(rule: ParserRule) { require(rule.keyword.isNotBlank()); rules.update(rule.copy(keyword = rule.keyword.trim())) }
    suspend fun deleteRule(rule: ParserRule) = rules.delete(rule)
    suspend fun getRulesSync() = rules.getAllRulesSync()
    suspend fun getAllTransactionsSync() = transactions.getAllTransactionsSync()
    suspend fun start(config: FinanceConfig, policy: BudgetPolicy) = database.withTransaction {
        require(config.openingBalance in 0..BudgetEngine.MAX_AMOUNT)
        ZoneId.of(config.timezone)
        require(policy.effectiveDay == config.startDay)
        validatePolicy(policy)
        transactions.clearAll(); rules.clearAll(); finance.clearNotifications()
        finance.clearAdjustments(); finance.clearPolicies(); finance.clearConfig()
        finance.saveConfig(config); finance.addPolicy(policy)
    }
    suspend fun addPolicy(policy: BudgetPolicy) {
        val config = requireNotNull(finance.config())
        require(policy.effectiveDay > LocalDate.now(ZoneId.of(config.timezone)).toEpochDay()) { "Budget baru mulai paling cepat besok" }
        validatePolicy(policy)
        finance.saveFuturePolicy(policy)
    }
    suspend fun deleteFuturePolicy(day: Long) {
        val config = requireNotNull(finance.config())
        require(day > LocalDate.now(ZoneId.of(config.timezone)).toEpochDay()) { "Budget yang sudah berlaku tidak bisa dibatalkan" }
        finance.deletePolicy(day)
    }
    suspend fun addAdjustment(adjustment: BalanceAdjustment) {
        require(finance.config() != null)
        require(adjustment.target in listOf("saldo", "extra") && adjustment.amount != 0L && adjustment.amount in -BudgetEngine.MAX_AMOUNT..BudgetEngine.MAX_AMOUNT)
        require(adjustment.note.isNotBlank()) { "Catatan koreksi wajib diisi" }
        finance.addAdjustment(adjustment)
    }
    suspend fun deleteAdjustment(adjustment: BalanceAdjustment) = finance.deleteAdjustment(adjustment)
    suspend fun backup() = FluxBackupData(transactions.getAllTransactionsSync(), rules.getAllRulesSync(), config = finance.config(), policies = finance.policies(), adjustments = finance.adjustments())
    suspend fun restoreData(backup: FluxBackupData) = database.withTransaction {
        require(backup.version == 2) { "Backup versi lama tidak didukung" }
        val config = requireNotNull(backup.config)
        ZoneId.of(config.timezone)
        val today = LocalDate.now(ZoneId.of(config.timezone)).toEpochDay()
        require(config.id == 1 && config.startDay in 0..today && config.openingBalance in 0..BudgetEngine.MAX_AMOUNT)
        require(backup.policies.any { it.effectiveDay == config.startDay })
        require(backup.policies.map { it.effectiveDay }.distinct().size == backup.policies.size)
        backup.policies.forEach { validatePolicy(it); require(it.effectiveDay >= config.startDay) }
        require(backup.transactions.map { it.id }.distinct().size == backup.transactions.size)
        backup.transactions.forEach { validateTransaction(it); require(BudgetEngine.day(it.date, config.timezone).toEpochDay() in config.startDay..today) }
        backup.rules.forEach { require(it.keyword.isNotBlank()) }
        backup.adjustments.forEach {
            require(it.target in listOf("saldo", "extra") && it.amount in -BudgetEngine.MAX_AMOUNT..BudgetEngine.MAX_AMOUNT && it.note.isNotBlank())
            require(BudgetEngine.day(it.date, config.timezone).toEpochDay() in config.startDay..today)
        }
        transactions.clearAll(); rules.clearAll(); finance.clearNotifications()
        finance.clearAdjustments(); finance.clearPolicies(); finance.clearConfig()
        finance.saveConfig(config.copy(activatedAt = System.currentTimeMillis()))
        backup.policies.forEach { finance.addPolicy(it) }
        transactions.insertAll(backup.transactions); rules.insertAll(backup.rules)
        backup.adjustments.forEach { finance.addAdjustment(it) }
    }
    /** Notification log and money record commit together. */
    suspend fun recordNotification(event: NotificationRecord): Boolean = recordNotificationDetail(event) != null
    suspend fun recordNotificationDetail(event: NotificationRecord): com.example.flux.notification.ParsedTransaction? = database.withTransaction {
        val config = finance.config() ?: return@withTransaction null
        if (event.postedAt < config.activatedAt) return@withTransaction null
        if (finance.hasEvent(event.eventId) > 0) return@withTransaction null
        if (finance.recordedOccurrence(event.occurrenceId) > 0) return@withTransaction null
        if (finance.similarRecorded(event.fingerprint, event.postedAt) > 0) {
            finance.addNotification(event.copy(status = "Perlu diperiksa", detail = "Notifikasi identik dalam 2 menit. Mungkin duplikat; catat manual jika ini transaksi lain."))
            return@withTransaction null
        }
        val customRules = rules.getAllRulesSync()
        val blocked = NotificationTransactionParser.blockedBy(event.title, event.text, customRules)
        val parsed = if (blocked == null) NotificationTransactionParser.parse(event.title, event.text, customRules) else null
        val status = when { blocked != null -> "Diabaikan"; parsed != null -> "Tercatat"; else -> "Perlu diperiksa" }
        val detail = blocked?.let { "Blacklist: ${it.keyword}" } ?: parsed?.let { "${it.category} · Rp ${it.amount.toLong()}" } ?: "Nominal atau jenis transaksi tidak dikenali. Tambahkan manual bila perlu."
        finance.addNotification(event.copy(status = status, detail = detail))
        if (parsed != null) transactions.insertTransaction(TransactionEntity(amount = parsed.amount, note = parsed.note, category = parsed.category, isIncome = parsed.isIncome, date = event.postedAt, source = "blu"))
        parsed
    }
    private fun validatePolicy(policy: BudgetPolicy) { require(policy.weeklyAmounts().all { it <= BudgetEngine.MAX_AMOUNT }) }
    private fun validateTransaction(tx: TransactionEntity) {
        require(tx.amount.isFinite() && tx.amount > 0 && tx.amount <= BudgetEngine.MAX_AMOUNT && tx.amount % 1.0 == 0.0) { "Nominal harus rupiah bulat positif" }
        require(tx.refund.isFinite() && tx.refund in 0.0..tx.amount && tx.refund % 1.0 == 0.0)
        require(!tx.isIncome || tx.refund == 0.0) { "Refund hanya untuk pengeluaran" }
        require(tx.refund == 0.0 || tx.refundNote.isNotBlank()) { "Jelaskan alasan refund" }
        require(tx.category.isNotBlank() && !tx.category.startsWith("Injection"))
    }
}
