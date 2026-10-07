package com.example.flux

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flux.data.TransactionDatabase
import com.example.flux.data.TransactionRepository
import com.example.flux.ui.DashboardScreen
import com.example.flux.ui.theme.FluxTheme
import com.example.flux.viewmodel.DashboardViewModel
import com.example.flux.viewmodel.DashboardViewModelFactory
import androidx.core.app.NotificationManagerCompat
import com.example.flux.notification.FluxNotificationListenerService

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        if (packageName in NotificationManagerCompat.getEnabledListenerPackages(this) && !FluxNotificationListenerService.health.value.connected) {
            FluxNotificationListenerService.reconnect(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.flux.preferences.AppPreferences.initialize(this)
        enableEdgeToEdge()

        val database = TransactionDatabase.getDatabase(this)
        val repository = TransactionRepository(database)
        val viewModelFactory = DashboardViewModelFactory(repository)

        setContent {
            FluxTheme {
                val viewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }
}
