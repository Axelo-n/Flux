package com.example.flux

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flux.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- DATA MODEL UI DUMMY (Disesuaikan pake Int/Drawable) ---
data class DummyTransaction(
    val title: String,
    val category: String,
    val amount: String,
    val isIncome: Boolean,
    val iconRes: Int, // Pake Int (R.drawable)
    val iconBg: Color
)

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    // 1. Ambil Data Real dari ViewModel
    val state by viewModel.uiState.collectAsState()
    val allTransactions = state.recentTransactions

    // --- LOGIKA FILTER WAKTU ---
    val calendar = Calendar.getInstance()

    // State untuk Filter (Default ke Bulan & Tahun Sekarang)
    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) } // 0 = Jan, 11 = Dec
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    // List Pilihan untuk Dropdown
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2024..2030).toList() // Bisa disesuaikan range tahunnya

    // 2. FILTERING DATA (Core Logic)
    // Kita filter data 'allTransactions' berdasarkan state bulan & tahun di atas
    val filteredTransactions = remember(allTransactions, selectedMonthIndex, selectedYear) {
        allTransactions.filter { tx ->
            val txCalendar = Calendar.getInstance()
            txCalendar.timeInMillis = tx.date // Asumsi tx.date adalah Long (timestamp)

            val txMonth = txCalendar.get(Calendar.MONTH)
            val txYear = txCalendar.get(Calendar.YEAR)

            txMonth == selectedMonthIndex && txYear == selectedYear
        }
    }

    // 3. GROUPING DATA (Berdasarkan Tanggal)
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions
            .sortedByDescending { it.date } // Urutkan dari yang terbaru
            .groupBy {
                SimpleDateFormat("dd", Locale.getDefault()).format(Date(it.date))
            }
    }

    // 4. HITUNG TOTAL SALDO (Hanya dari data yang sudah difilter)
    val totalBalance = remember(filteredTransactions) {
        filteredTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    // Helper Format Rupiah
    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    Scaffold(
        containerColor = UIBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp)
            ) {
                // Tombol Close
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(UISurface)
                        .clickable { onBack() }
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close), // Pastikan ada di drawable
                        contentDescription = "Close",
                        tint = UIWhite
                    )
                }

                // Total Balance & Filter Aktif
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tampilkan Total Balance Hasil Perhitungan
                    Text(
                        text = formatRupiah(totalBalance),
                        style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // --- DROPDOWN BULAN ---
                        DropdownFilter(
                            label = months[selectedMonthIndex],
                            items = months,
                            onItemSelected = { index, _ -> selectedMonthIndex = index }
                        )

                        // --- DROPDOWN TAHUN ---
                        DropdownFilter(
                            label = selectedYear.toString(),
                            items = years.map { it.toString() },
                            onItemSelected = { _, item -> selectedYear = item.toInt() }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 170.dp, start = 10.dp, end = 10.dp)
                .clip(RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp))
                .background(UISurface)
        ) {
            if (filteredTransactions.isEmpty()) {
                // Tampilan kalau data kosong di bulan tsb
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions in this period", style = AppFont.Regular.copy(color = UIGray))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (date, txList) ->
                        item { DateHeaderUI(date) }
                        items(txList) { transaction ->
                            TransactionItemUI(
                                data = transaction,
                                onClick = {
                                    navController.navigate("edit_transaction/${transaction.id}")
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(10.dp)) }
                    }
                }
            }
        }
    }
}

// --- KOMPONEN DROPDOWN CUSTOM ---
@Composable
fun DropdownFilter(
    label: String,
    items: List<String>,
    onItemSelected: (index: Int, item: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        // Tombol Pill (Trigger)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(brush = gradientBrush)
                .clickable { expanded = true } // Buka menu pas diklik
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppFont.Bold.copy(fontSize = 14.sp, color = UIBackground)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_dropdown), // Pastikan ada icon dropdown/arrow down
                contentDescription = null,
                tint = UIBackground,
                modifier = Modifier.size(10.dp)
            )
        }

        // Menu Dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(UISurface).heightIn(max = 200.dp) // Max height biar bisa scroll
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(text = item, color = if(item == label) UITeal else UIWhite)
                    },
                    onClick = {
                        onItemSelected(index, item)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun FilterPillUI(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(brush = gradientBrush)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AppFont.Bold.copy(fontSize = 14.sp, color = UIBackground)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_dropdown),
            contentDescription = null,
            tint = UIBackground,
            modifier = Modifier.size(8.dp)
        )
    }
}

@Composable
fun DateHeaderUI(date: String) {
    Text(
        text = date,
        style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun TransactionItemUI(
    data: Transaction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(data.iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // UPDATE: Pake painterResource karena sekarang datanya Int (Drawable)
                painter = painterResource(id = data.iconRes),
                contentDescription = null,
                tint = UIWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title & Category
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = AppFont.Medium.copy(fontSize = 18.sp, color = UIWhite),
                modifier = Modifier.offset(y = (2).dp)
            )
            Text(
                text = data.category,
                style = AppFont.Regular.copy(fontSize = 14.sp, color = UIGray),
                modifier = Modifier.offset(y = (-2).dp)
            )
        }

        // Amount
        Text(
            text = data.formattedAmount, // GANTI: Pake formattedAmount dari ViewModel
            style = AppFont.Bold.copy(
                fontSize = 18.sp,
                color = if (data.isIncome) UIGreen else UIRed
            )
        )
    }
}

// --- DATA DUMMY GENERATOR ---

//
//@Preview(showBackground = true)
//@Composable
//fun HistoryScreenPreview() {
//    FluxTheme {
//        HistoryScreen()
//    }
//}