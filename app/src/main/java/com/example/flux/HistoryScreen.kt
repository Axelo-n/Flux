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
import androidx.compose.runtime.remember
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
    viewModel: DashboardViewModel, // Tambah ini
    navController: NavController,  // Tambah ini
    onBack: () -> Unit             // Tambah ini
) {
    // 1. Ambil Data Real
    val state by viewModel.uiState.collectAsState()
    val transactions = state.recentTransactions

    // 2. Grouping Data (Simulasi tanggal hari ini dulu)
    val groupedTransactions = remember(transactions) {
        transactions.groupBy {
            // Nanti ganti ini dengan tanggal asli dari DB
            "Today"
        }
    }

    // 3. Hitung Total Saldo Real
    val totalBalance = remember(transactions) {
        transactions.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    Scaffold(
        containerColor = UIBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp)
            ) {
                // 1. Tombol Close
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(UISurface)
                        .clickable { /* Aksi Dummy */ }
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = UIWhite
                    )
                }

                // 2. Total Balance & Filter
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rp 1.234.000",
                        style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterPillUI(text = "January")
                        FilterPillUI(text = "2026")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 170.dp, start = 10.dp, end = 10.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 35.dp,
                        topEnd = 35.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                        )
                    )
                .background(UISurface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Loop setiap grup tanggal
                groupedTransactions.forEach { (date, txList) ->

                    // Header Tanggal
                    item { DateHeaderUI(date) }

                    // List Item di tanggal tersebut
                    items(txList) { transaction ->
                        TransactionItemUI(
                            data = transaction,
                            onClick = {
                                // NAVIGASI KE HALAMAN EDIT
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