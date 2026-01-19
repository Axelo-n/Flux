package com.example.flux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.flux.ui.theme.FluxTheme
// Kalau DashboardScreen ada di folder ui/screen, uncomment baris bawah ini:
// import com.example.flux.ui.screen.DashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Full Screen Mode
        // Ini bikin app lo nge-draw sampai ke belakang jam/baterai (Status Bar)
        // Biar kelihatan modern & luas.
        enableEdgeToEdge()

        setContent {
            // 2. Bungkus dengan FluxTheme
            // Penting biar warna Background gelap (0xFF0B0E14) otomatis kepasang
            FluxTheme {
                // 3. Panggil Layar Utama
                // Karena tadi kita udah pasang NavHost di dalem DashboardScreen,
                // Navigasi bakal langsung jalan dari sini.
                DashboardScreen()
            }
        }
    }
}