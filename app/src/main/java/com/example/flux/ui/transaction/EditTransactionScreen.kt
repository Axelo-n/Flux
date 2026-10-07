package com.example.flux.ui.transaction

import androidx.compose.runtime.*
import com.example.flux.viewmodel.DashboardViewModel

@Composable
fun EditTransactionScreen(viewModel: DashboardViewModel, transactionId: Int, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val transaction = state.recentTransactions.find { it.id == transactionId }
    if (transaction != null) TransactionEditor(viewModel, transaction, onBack)
    else if (!state.isLoading) LaunchedEffect(transactionId) { onBack() }
}
