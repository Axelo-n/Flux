package com.example.flux.ui.history

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flux.model.BudgetEngine
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.viewmodel.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: DashboardViewModel, navController: NavController, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()
    val zone = config?.timezone ?: "Asia/Jakarta"
    var month by remember { mutableStateOf(YearMonth.now(ZoneId.of(zone))) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    val monthly = state.recentTransactions.filter { YearMonth.from(BudgetEngine.day(it.date, zone)) == month }
    val rows = monthly.filter { (filter == "Semua" || if (filter == "Income") it.isIncome else !it.isIncome) && (it.title.contains(search, true) || it.category.contains(search, true) || it.refundNote.contains(search, true)) }
    val grouped = rows.groupBy { BudgetEngine.day(it.date, zone) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("Riwayat", "Setiap transaksi, tersimpan rapi.") }
        item {
            MonthSelector(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", com.example.flux.preferences.AppPreferences.locale)), { month = month.minusMonths(1) }, { month = month.plusMonths(1) })
        }
        item { Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Hint("Pemasukan"); MoneyValue(monthly.filter { it.isIncome }.sumOf { it.amount }.toLong(), UIGreen, 24.sp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Hint("Pengeluaran"); MoneyValue(monthly.filter { !it.isIncome }.sumOf { it.amount }.toLong(), UIWhite, 24.sp) }
            }
        } }
        item { OutlinedTextField(search, { search = it }, placeholder = { Text(translate("Cari catatan atau kategori")) }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(16.dp), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Semua", "Expense", "Income").forEach { option -> FilterChip(filter == option, { filter = option }, { Text(translate(option)) }) } } }
        if (rows.isEmpty()) item { Panel { Hint("Belum ada transaksi yang cocok.") } }
        grouped.forEach { (date, txs) ->
            item { Text(translate(date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", com.example.flux.preferences.AppPreferences.locale))), color = UIGray, style = AppFont.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
            items(txs, key = { it.id }) { tx -> Surface(color = UISurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .5f))) { TransactionItem(tx) { navController.navigate("edit_transaction/${tx.id}") } } }
        }
    }
}
