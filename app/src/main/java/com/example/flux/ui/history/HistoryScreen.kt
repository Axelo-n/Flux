package com.example.flux.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flux.R
import com.example.flux.ui.components.DropdownFilter
import com.example.flux.ui.components.TransactionItem
import com.example.flux.ui.theme.AppFont
import com.example.flux.ui.theme.UIBackground
import com.example.flux.ui.theme.UIGray
import com.example.flux.ui.theme.UISurface
import com.example.flux.ui.theme.UIWhite
import com.example.flux.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val calendar = Calendar.getInstance()

    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2024..2030).map { it.toString() }

    val filteredTransactions = remember(state.recentTransactions, selectedMonthIndex, selectedYear) {
        state.recentTransactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            txCal.get(Calendar.MONTH) == selectedMonthIndex && txCal.get(Calendar.YEAR) == selectedYear
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions
            .sortedByDescending { it.date }
            .groupBy { SimpleDateFormat("dd", Locale.getDefault()).format(Date(it.date)) }
    }

    val totalBalance = remember(filteredTransactions) {
        filteredTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }.format(amount).replace("Rp", "Rp ")
    }

    Scaffold(
        containerColor = UIBackground,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 20.dp, end = 20.dp)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(UISurface)
                        .clickable { onBack() }
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Close", tint = UIWhite)
                }

                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatRupiah(totalBalance), style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownFilter(
                            label = months[selectedMonthIndex],
                            items = months,
                            onItemSelected = { index, _ -> selectedMonthIndex = index }
                        )
                        DropdownFilter(
                            label = selectedYear.toString(),
                            items = years,
                            onItemSelected = { _, item -> selectedYear = item.toInt() }
                        )
                    }
                }
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 170.dp, start = 10.dp, end = 10.dp)
                .clip(RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp))
                .background(UISurface)
        ) {
            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions in this period", style = AppFont.Regular.copy(color = UIGray))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (date, txList) ->
                        item {
                            Text(text = date, style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite), modifier = Modifier.padding(bottom = 2.dp))
                        }
                        items(txList) { transaction ->
                            TransactionItem(
                                data = transaction,
                                onClick = { navController.navigate("edit_transaction/${transaction.id}") }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(10.dp)) }
                    }
                }
            }
        }
    }
}
