package com.example.flux.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Kita paksa pake Dark Scheme karena desain Flux emang dark mode
private val DarkColorScheme = darkColorScheme(
    primary = UITeal,        // Warna utama (Progress bar, Button aktif)
    onPrimary = UIBackground,// Warna teks di atas tombol primary
    secondary = UIBlue,      // Warna aksen kedua
    onSecondary = UIWhite,

    background = UIBackground, // 0xFF0B0E14
    onBackground = UIWhite,    // Teks utama

    surface = UISurface,       // 0xFF21262D (Buat Card/TopBar)
    onSurface = UIWhite,       // Teks di atas Card

    error = UIRed,
    onError = UIWhite,

    outline = UIGray           // Buat garis pinggir/border tipis
)

@Composable
fun FluxTheme(
    // Kita hapus parameter dynamicColor biar konsisten pake warna kita
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Bikin status bar nyatu sama background
            window.statusBarColor = colorScheme.background.toArgb()
            // Icon status bar putih (karena background gelap)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Typography default Material tetep jalan, tapi kita bakal sering pake AppFont manual
        typography = MaterialTheme.typography,
        content = content
    )
}