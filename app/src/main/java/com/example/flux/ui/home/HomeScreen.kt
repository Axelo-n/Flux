package com.example.flux.ui.home

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.flux.viewmodel.DashboardViewModel

@Composable
fun HomeScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isListenerActive by remember { mutableStateOf(false) }

    fun checkListenerStatus() {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isListenerActive = flat != null && flat.contains(context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkListenerStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkListenerStatus()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 150.dp, top = 20.dp)
    ) {
        item { HeaderSection(isActive = isListenerActive) }
        item { BudgetGridSection(dailyLeft = state.dailyBudgetLeft, dailyUsagePercent = state.dailyUsagePercent) }
        item { BalanceRowSection(currentBalance = state.currentBalance, extraBalance = state.extraBalance, isPositive = state.isExtraBalancePositive) }
        item { SpendingGraphSection(dataPoints = state.graphData) }
        item { RecentTransactionsCard(transactions = state.recentTransactions) }
        item { BottomSpacer() }
    }
}
