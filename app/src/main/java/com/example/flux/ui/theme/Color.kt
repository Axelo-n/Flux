package com.example.flux.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base Colors
val UIBlack = Color(0xFF000000)
val UIWhite = Color(0xFFFFFFFF)

// Backgrounds
val UIBackground = Color(0xFF0B0E14) // Gue rename dikit biar jelas ini buat Background
val UISurface = Color(0xFF21262D)    // Buat Card/Container

// Accents
val UITeal = Color(0xFF4FD1C5)       // Primary Brand Color (Flux Cyan)
val UIBlue = Color(0xFF6366F1)       // Secondary Accent (Electric Indigo)

// Semantic / Text Colors
val UITertiary = Color(0xFF475569)   // Disabled / Low emphasis
val UIGray = Color(0xFF94A3B8)       // Subtitle / Medium emphasis
val UIGreen = Color(0xFF11F35E)      // Success
val UIRed = Color(0xFFEC2C51)        // Error / Overbudget

val CatBlue = Color(0xFF4F42E4)
val CatOrange = Color(0xFFFF7700) // Warna Orange Warung
val CatPurple = Color(0xFF6C5DD3) // Warna Ungu Transfer/Shop
val CatGreen = Color(0xFF00CB6A)
val CatYellow = Color(0xFFF6B200)
val CatCyan = Color(0xFF0090FE)
val CatGrey = Color(0xFF94A3B8)

val gradientBrush = Brush.linearGradient(
    colors = listOf(UIBlue, UITeal),
    start = Offset.Zero,
    end = Offset.Infinite
)
