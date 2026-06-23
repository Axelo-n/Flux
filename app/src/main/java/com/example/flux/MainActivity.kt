package com.example.flux

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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

                viewModel.addTransaction(amount, note, category, isIncome)

                Toast.makeText(context, "Flux detected: $note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Variable viewModel
    private lateinit var viewModel: DashboardViewModel

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Siapkan Database & Repository
        val database = TransactionDatabase.getDatabase(this)
        val repository = TransactionRepository(database.transactionDao(), database.parserRuleDao())

        // 2. Siapkan Factory
        val viewModelFactory = DashboardViewModelFactory(repository)

        // DAFTARKAN ANTENA (RECEIVER)
        val filter = IntentFilter("com.example.flux.NEW_TRANSACTION_DETECTED")
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(transactionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(transactionReceiver, filter)
        }

        setContent {
            FluxTheme {
                val viewModel: DashboardViewModel = viewModel(factory = viewModelFactory)

                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(transactionReceiver)
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}