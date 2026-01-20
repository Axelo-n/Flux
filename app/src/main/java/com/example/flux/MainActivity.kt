package com.example.flux

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flux.ui.theme.FluxTheme

class MainActivity : ComponentActivity() {
    // Antena Penerima
    private val transactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.flux.NEW_TRANSACTION_DETECTED") {
                val amount = intent.getDoubleExtra("amount", 0.0)
                val category = intent.getStringExtra("category") ?: "Other"
                val note = intent.getStringExtra("note") ?: "Auto-detected"
                val isIncome = intent.getBooleanExtra("isIncome", false)

                // SURUH VIEWMODEL NYIMPEN
                // (Kita butuh akses ke ViewModel disini. Karena pake factory manual,
                // kita bisa akses lewat variable yg udah di-init di onCreate)
                viewModel.addTransaction(amount, note, category, isIncome)

                Toast.makeText(context, "Flux detected: $note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Variable viewModel biar bisa diakses receiver
    private lateinit var viewModel: DashboardViewModel

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Siapkan Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = TransactionRepository(database.transactionDao())

        // 2. Siapkan Factory
        val viewModelFactory = DashboardViewModelFactory(repository)

        // DAFTARKAN ANTENA (RECEIVER)
        // Android 13+ butuh flag RECEIVER_EXPORTED atau NOT_EXPORTED
        val filter = IntentFilter("com.example.flux.NEW_TRANSACTION_DETECTED")
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(transactionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(transactionReceiver, filter)
        }

        setContent {
            FluxTheme {
                // 3. Kita inject ViewModel pakai Factory ini ke DashboardScreen
                // Caranya: Kita panggil ViewModel-nya DISINI, lalu oper ke DashboardScreen
                val viewModel: DashboardViewModel = viewModel(factory = viewModelFactory)

                DashboardScreen(viewModel = viewModel)
            }
        }

        fun onDestroy() {
            super.onDestroy()
            // Matikan antena pas aplikasi ditutup total
            unregisterReceiver(transactionReceiver)
        }
    }
}