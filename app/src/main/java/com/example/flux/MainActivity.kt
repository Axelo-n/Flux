package com.example.flux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flux.ui.theme.FluxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Siapkan Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = TransactionRepository(database.transactionDao())

        // 2. Siapkan Factory
        val viewModelFactory = DashboardViewModelFactory(repository)

        setContent {
            FluxTheme {
                // 3. Kita inject ViewModel pakai Factory ini ke DashboardScreen
                // Caranya: Kita panggil ViewModel-nya DISINI, lalu oper ke DashboardScreen
                val viewModel: DashboardViewModel = viewModel(factory = viewModelFactory)

                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}