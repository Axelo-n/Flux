package com.example.flux.ui.transaction

import androidx.compose.runtime.Composable
import com.example.flux.viewmodel.DashboardViewModel

@Composable
fun AddTransactionScreen(viewModel: DashboardViewModel, onBack: () -> Unit) = TransactionEditor(viewModel, null, onBack)
