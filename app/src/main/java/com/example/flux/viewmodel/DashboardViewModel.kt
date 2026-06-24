package com.example.flux.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.flux.R
import com.example.flux.data.FluxBackupData
import com.example.flux.data.TransactionEntity
import com.example.flux.data.TransactionRepository
import com.example.flux.model.AnalyticsState
import com.example.flux.model.CategoryStat
import com.example.flux.model.DashboardState
import com.example.flux.model.DayData
import com.example.flux.model.MonthlyAnalyticsState
import com.example.flux.model.Transaction
import com.example.flux.ui.theme.CatBlue
import com.example.flux.ui.theme.CatGreen
import com.example.flux.ui.theme.CatGrey
import com.example.flux.ui.theme.CatOrange
import com.example.flux.ui.theme.CatPurple
import com.example.flux.ui.theme.CatYellow
import com.example.flux.ui.theme.UIBlue
import com.example.flux.ui.theme.UITeal
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    val uiState: StateFlow<DashboardState> = combine(
        repository.allTransactions,
        repository.totalIncome,
        repository.totalExpense
    ) { transactions, _, _ ->
        val allUiTransactions = transactions.map { entity ->
            val (iconId, color) = getCategoryStyle(entity.category, entity.isIncome)
            Transaction(
                id = entity.id,
                title = entity.note.ifEmpty { entity.category },
                category = entity.category,
                amount = entity.amount,
                formattedAmount = formatRupiah(entity.amount),
                iconRes = iconId,
                iconBgColor = color,
                isIncome = entity.isIncome,
                date = entity.date
            )
        }

        val analytics = calculateAnalytics(allUiTransactions)
        val cleanTransactions = allUiTransactions.filter { !it.category.startsWith("Injection") }

        DashboardState(
            isLoading = false,
            recentTransactions = cleanTransactions,
            dailyBudgetLeft = analytics.dailyLeft,
            dailyUsagePercent = analytics.dailyUsagePercent,
            currentBalance = analytics.currentBalance,
            extraBalance = analytics.extraBalance,
            isExtraBalancePositive = analytics.isExtraPositive,
            graphData = analytics.graphData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    // --- CRUD ---

    fun addTransaction(amount: Double, note: String, category: String, isIncome: Boolean) {
        viewModelScope.launch {
            repository.insert(TransactionEntity(amount = amount, note = note, category = category, isIncome = isIncome, date = System.currentTimeMillis()))
        }
    }

    fun updateTransaction(id: Int, amount: Double, note: String, category: String, isIncome: Boolean, date: Long) {
        viewModelScope.launch {
            repository.insert(TransactionEntity(id = id, amount = amount, note = note, category = category, isIncome = isIncome, date = date))
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun getTransactionById(id: Int): Transaction? = uiState.value.recentTransactions.find { it.id == id }

    // --- PARSER RULES ---

    val parserRules = repository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addParserRule(keyword: String, category: String, note: String?) {
        viewModelScope.launch {
            repository.insertRule(com.example.flux.data.ParserRule(keyword = keyword, targetCategory = category, targetNote = note))
        }
    }

    fun deleteParserRule(rule: com.example.flux.data.ParserRule) {
        viewModelScope.launch { repository.deleteRule(rule) }
    }

    // --- BALANCE INJECTION ---

    fun injectCurrentBalance(amount: Double) {
        viewModelScope.launch {
            repository.insert(TransactionEntity(amount = amount, note = "Manual Injection (Current)", category = "Injection_Current", isIncome = true, date = System.currentTimeMillis()))
        }
    }

    fun injectExtraBalance(amount: Double) {
        viewModelScope.launch {
            repository.insert(TransactionEntity(amount = amount, note = "Manual Injection (Extra)", category = "Injection_Extra", isIncome = true, date = System.currentTimeMillis()))
        }
    }

    // --- BACKUP / RESTORE ---

    suspend fun createBackupJson(): String {
        val transactions = repository.getAllTransactionsSync()
        val rules = repository.getRulesSync()
        return Gson().toJson(FluxBackupData(transactions, rules))
    }

    fun restoreFromBackup(jsonString: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = Gson().fromJson(jsonString, FluxBackupData::class.java)
                repository.restoreData(backupData)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }

    // --- ANALYTICS ---

    private val _analyticsDate = MutableStateFlow(Calendar.getInstance())
    val analyticsDate = _analyticsDate.asStateFlow()

    fun nextMonth() {
        _analyticsDate.update { (it.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }
    }

    fun prevMonth() {
        _analyticsDate.update { (it.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }
    }

    fun getMonthlyAnalytics(selectedDate: Calendar, allTransactions: List<Transaction>): MonthlyAnalyticsState {
        val targetMonth = selectedDate.get(Calendar.MONTH)
        val targetYear = selectedDate.get(Calendar.YEAR)

        val monthlyTx = allTransactions.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.MONTH) == targetMonth &&
                c.get(Calendar.YEAR) == targetYear &&
                !it.category.startsWith("Injection")
        }

        val totalExpense = monthlyTx.filter { !it.isIncome }.sumOf { it.amount }
        val totalIncome = monthlyTx.filter { it.isIncome }.sumOf { it.amount }

        val groupedStats = monthlyTx.filter { !it.isIncome }
            .groupBy { it.category }
            .map { (cat, list) ->
                val catTotal = list.sumOf { it.amount }
                val percent = if (totalExpense > 0) (catTotal / totalExpense).toFloat() else 0f
                val (icon, color) = getCategoryStyle(cat, false)
                CategoryStat(cat, catTotal, percent, color, icon)
            }
            .sortedByDescending { it.percentage }

        val maxDays = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyGraphData = (1..maxDays).map { day ->
            val expenseThatDay = monthlyTx.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.date }
                !it.isIncome && c.get(Calendar.DAY_OF_MONTH) == day
            }.sumOf { it.amount }

            val checkDate = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            val limit = if (checkDate.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) 60000f else 40000f

            DayData(day = day.toString(), amount = expenseThatDay.toFloat(), limit = limit)
        }

        return MonthlyAnalyticsState(
            totalExpense = formatRupiah(totalExpense),
            totalIncome = formatRupiah(totalIncome),
            categoryStats = groupedStats,
            dailyGraphData = dailyGraphData
        )
    }

    fun calculateAnalytics(transactions: List<Transaction>): AnalyticsState {
        val calendar = Calendar.getInstance()
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        if (transactions.isEmpty()) {
            val emptyGraph = (0..6).map { i ->
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - i)) }
                val limit = if (c.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) 60000f else 40000f
                DayData(SimpleDateFormat("EEE", Locale.getDefault()).format(c.time), 0f, limit)
            }
            return AnalyticsState("Rp 0", 0f, "Rp 0", "Rp 0", true, emptyGraph)
        }

        val txByDay = transactions.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
        }

        var totalIncomeForCurrent = 0.0
        var totalExpense = 0.0
        var extraBalance = 0.0

        val startCal = Calendar.getInstance().apply {
            timeInMillis = transactions.minOf { it.date }
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        while (startCal <= endCal) {
            val dKey = "${startCal.get(Calendar.DAY_OF_YEAR)}-${startCal.get(Calendar.YEAR)}"
            val txList = txByDay[dKey] ?: emptyList()
            val isToday = (startCal == endCal)
            val dailyLimit = if (startCal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) 60000.0 else 40000.0

            val incomeForCurrent = txList.filter { it.isIncome && it.category != "Injection_Extra" }.sumOf { it.amount }
            val incomeForExtra = txList.filter { it.isIncome && it.category != "Injection_Current" }.sumOf { it.amount }
            val daysExpense = txList.filter { !it.isIncome }.sumOf { it.amount }

            totalIncomeForCurrent += incomeForCurrent
            totalExpense += daysExpense
            extraBalance += incomeForExtra

            val dailyLeft = dailyLimit - daysExpense
            if (isToday) {
                if (dailyLeft < 0) extraBalance += dailyLeft
            } else {
                extraBalance += dailyLeft
            }

            startCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dayOfWeekToday = calendar.get(Calendar.DAY_OF_WEEK)
        val limitToday = if (dayOfWeekToday in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) 60000.0 else 40000.0
        val todayTx = transactions.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_YEAR) == todayDay && c.get(Calendar.YEAR) == todayYear
        }
        val expenseToday = todayTx.filter { !it.isIncome }.sumOf { it.amount }
        val dailyLeftToday = limitToday - expenseToday
        val dailyUsagePercent = (expenseToday / limitToday).toFloat().coerceIn(0f, 1f)

        val graphData = (0..6).map { i ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - i)) }
            val dKey = "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
            val limit = if (c.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) 60000f else 40000f
            val dExpense = (txByDay[dKey] ?: emptyList()).filter { !it.isIncome }.sumOf { it.amount }.toFloat()
            DayData(SimpleDateFormat("EEE", Locale.getDefault()).format(c.time), dExpense, limit)
        }

        return AnalyticsState(
            dailyLeft = formatRupiah(dailyLeftToday),
            dailyUsagePercent = dailyUsagePercent,
            currentBalance = formatRupiah(totalIncomeForCurrent - totalExpense),
            extraBalance = formatRupiah(extraBalance),
            isExtraPositive = extraBalance >= 0,
            graphData = graphData
        )
    }

    // --- HELPERS ---

    private fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }.format(amount).replace("Rp", "Rp ")
    }

    private fun getCategoryStyle(category: String, isIncome: Boolean): Pair<Int, Color> {
        if (isIncome) return Pair(R.drawable.ic_wallet_outline, CatBlue)

        return when (category) {
            "Food and Beverages"      -> Pair(R.drawable.ic_food_outline, CatOrange)
            "Transportation"          -> Pair(R.drawable.ic_car_outline, CatGreen)
            "Groceries and Shopping"  -> Pair(R.drawable.ic_cart_outline, CatPurple)
            "Entertainment"           -> Pair(R.drawable.ic_ticket_outline, CatYellow)
            "Account Transfer"        -> Pair(R.drawable.ic_card_outline, CatBlue)
            "Other"                   -> Pair(R.drawable.ic_other_outline, CatGrey)
            "Injection_Current"       -> Pair(R.drawable.ic_wallet_outline, UITeal)
            "Injection_Extra"         -> Pair(R.drawable.ic_wallet_outline, UIBlue)
            "DEBUG_LOG"               -> Pair(R.drawable.ic_other_outline, Color.Red)
            else                      -> Pair(R.drawable.ic_other_outline, CatGrey)
        }
    }
}

class DashboardViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
