package com.example.flux.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.*
import com.example.flux.R
import com.example.flux.data.*
import com.example.flux.model.*
import com.example.flux.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.*
import java.util.Calendar
import java.util.Locale

fun rupiah(amount: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }.format(amount).replace("Rp", "Rp ")

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {
    val monthlyPockets = repository.monthlyPockets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun saveMonthlyPocket(pocket: MonthlyPocket, done: () -> Unit) = perform("Pocket tersimpan", done) { repository.saveMonthlyPocket(pocket) }
    fun deleteMonthlyPocket(pocket: MonthlyPocket, done: () -> Unit) = perform("Pocket dihapus", done) { repository.deleteMonthlyPocket(pocket) }
    val config = repository.config.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val policies = repository.policies.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val adjustments = repository.adjustments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val parserRules = repository.allRules.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pendingCashbacks = repository.pendingCashbacks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notifications = repository.notifications.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val clock = flow { while (true) { emit(System.currentTimeMillis()); delay(15_000) } }
    private data class Inputs(val rows: List<TransactionEntity>, val config: FinanceConfig?, val policies: List<BudgetPolicy>, val adjustments: List<BalanceAdjustment>)
    private val inputs = combine(repository.allTransactions, repository.config, repository.policies, repository.adjustments) { t, c, p, a -> Inputs(t, c, p, a) }
    val uiState = combine(inputs, clock, androidx.compose.runtime.snapshotFlow { com.example.flux.preferences.AppPreferences.state.value }) { input, now, _ ->
        val today = BudgetEngine.day(now, input.config?.timezone ?: "Asia/Jakarta")
        val totals = BudgetEngine.calculate(input.config, input.policies, input.rows, input.adjustments, today)
        val transactions = if (input.config == null) emptyList() else input.rows.filter { !it.category.startsWith("Injection") }.map { entity ->
            val (icon, color) = style(entity.category, entity.isIncome)
            val effective = entity.amount - entity.refund
            Transaction(entity.id, entity.note.ifBlank { entity.category }, entity.category, effective, rupiah(effective.toLong()), icon, color, entity.isIncome, entity.date, entity.amount, entity.note, entity.refund, entity.refundNote, entity.source)
        }
        DashboardState(false, transactions, rupiah(totals.balance), rupiah(totals.extra), totals.extra >= 0, rupiah(totals.remaining), if (totals.budget > 0) (totals.spent.toFloat() / totals.budget).coerceIn(0f, 1f) else if (totals.spent > 0) 1f else 0f, totals.graph, totals, input.config != null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardState())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun clearMessage() { _message.value = null }
    private fun perform(success: String? = null, done: () -> Unit = {}, action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { action(); _message.value = success; done() }
            catch (e: Exception) { _message.value = e.message ?: "Tidak berhasil menyimpan. Coba lagi." }
            finally { _busy.value = false }
        }
    }
    fun startSystem(balance: Long, timezone: String, amounts: List<Long>, done: () -> Unit = {}) = perform("Sistem baru siap", done) {
        val today = LocalDate.now(ZoneId.of(timezone)).toEpochDay()
        repository.start(FinanceConfig(startDay = today, timezone = timezone, openingBalance = balance), BudgetPolicy(today, amounts.joinToString(",")))
    }
    fun saveBudget(date: LocalDate, amounts: List<Long>, done: () -> Unit) = perform("Budget baru dijadwalkan", done) { repository.addPolicy(BudgetPolicy(date.toEpochDay(), amounts.joinToString(","))) }
    fun cancelBudget(day: Long) = perform("Jadwal budget dibatalkan") { repository.deleteFuturePolicy(day) }
    fun saveTransaction(id: Int = 0, amount: Double, note: String, category: String, income: Boolean, date: Long, refund: Double = 0.0, refundNote: String = "", source: String = "manual", done: () -> Unit) = perform("Transaksi tersimpan", done) {
        repository.insert(TransactionEntity(id, amount, note.trim(), category, income, date, refund, refundNote.trim(), source))
    }
    fun deleteTransaction(id: Int, done: () -> Unit = {}) = perform("Transaksi dihapus", done) { repository.delete(id) }
    fun getTransactionById(id: Int) = uiState.value.recentTransactions.find { it.id == id }
    fun saveRule(rule: ParserRule, done: () -> Unit = {}) = perform("Aturan tersimpan", done) { if (rule.id == 0) repository.insertRule(rule) else repository.updateRule(rule) }
    fun dismissCashback(eventId: String) = perform("Cashback ditandai selesai") { repository.dismissCashback(eventId) }
    fun linkCashback(eventId: String, transactionId: Int, done: () -> Unit) = perform("Cashback ditautkan", done) { repository.linkCashback(eventId, transactionId) }
    fun deleteParserRule(rule: ParserRule) = perform("Aturan dihapus") { repository.deleteRule(rule) }
    fun adjust(target: String, amount: Long, note: String, done: () -> Unit) = perform("Koreksi tersimpan", done) { repository.addAdjustment(BalanceAdjustment(target = target, amount = amount, note = note.trim())) }
    fun deleteAdjustment(row: BalanceAdjustment) = perform("Koreksi dihapus") { repository.deleteAdjustment(row) }
    suspend fun createBackupJson(): String = Gson().toJson(repository.backup())
    fun restoreFromBackup(jsonString: String, onSuccess: () -> Unit, onError: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                // Check version explicitly; Gson does not invoke Kotlin defaults on old JSON.
                val tree = com.google.gson.JsonParser.parseString(jsonString).asJsonObject
                require(tree.get("version")?.asInt == 2)
                repository.restoreData(Gson().fromJson(tree, FluxBackupData::class.java)); onSuccess()
            } catch (e: Exception) { _message.value = "Backup tidak valid; data saat ini tetap aman."; onError() }
            finally { _busy.value = false }
        }
    }
    private val _analyticsDate = MutableStateFlow(Calendar.getInstance())
    val analyticsDate = _analyticsDate.asStateFlow()
    fun nextMonth() { _analyticsDate.update { (it.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, 1) } } }
    fun prevMonth() { _analyticsDate.update { (it.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -1) } } }
    fun getMonthlyAnalytics(selectedDate: Calendar, allTransactions: List<Transaction>): MonthlyAnalyticsState {
        val zone = config.value?.timezone ?: "Asia/Jakarta"
        val month = YearMonth.of(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH) + 1)
        val rows = allTransactions.filter { YearMonth.from(BudgetEngine.day(it.date, zone)) == month }
        val expense = rows.filter { !it.isIncome }.sumOf { it.amount }
        val stats = rows.filter { !it.isIncome && it.amount > 0 }.groupBy { it.category }.map { (cat, tx) ->
            val total = tx.sumOf { it.amount }; val (icon, color) = style(cat, false)
            CategoryStat(cat, total, if (expense > 0) (total / expense).toFloat() else 0f, color, icon)
        }.sortedByDescending { it.total }
        val graph = (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            val amount = rows.filter { !it.isIncome && BudgetEngine.day(it.date, zone) == date }.sumOf { it.amount }
            DayData(d.toString(), amount.toFloat(), BudgetEngine.budget(date, policies.value).toFloat())
        }
        return MonthlyAnalyticsState(rupiah(expense.toLong()), rupiah(rows.filter { it.isIncome }.sumOf { it.amount }.toLong()), stats, graph)
    }
    private fun style(category: String, income: Boolean): Pair<Int, Color> {
        val visual = categoryVisual(if (income) "Income" else category)
        return visual.icon to visual.color
    }

}
class DashboardViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
        @Suppress("UNCHECKED_CAST") return DashboardViewModel(repository) as T
    }
}
