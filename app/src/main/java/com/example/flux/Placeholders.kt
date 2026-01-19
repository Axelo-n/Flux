package com.example.flux

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.*

// Ini cuma buat tes navigasi
@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite)
        )
    }
}

// Daftar nama rute biar ga typo
object FluxRoutes {
    const val HOME = "home"
    const val ANALYTICS = "analytics"
    const val HISTORY = "history"
    const val WALLET = "wallet"
    const val ADD_TRANSACTION = "add_transaction" // <--- TAMBAH INI
}