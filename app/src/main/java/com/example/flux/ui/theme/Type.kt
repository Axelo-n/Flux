package com.example.flux.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.flux.R

// 1. Daftarkan Font Family
val AfacadFamily = FontFamily(
    Font(R.font.afacad_regular, FontWeight.Normal),
    Font(R.font.afacad_medium, FontWeight.Medium),
    Font(R.font.afacad_semibold, FontWeight.SemiBold),
    Font(R.font.afacad_bold, FontWeight.Bold),
    // Kalau mau pake italic juga, bisa ditambahin disini:
    // Font(R.font.afacad_italic, FontWeight.Normal, FontStyle.Italic)
)

// 2. Shortcut AppFont biar gampang dipanggil
object AppFont {
    val Regular = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp // Default size, bisa di-override nanti
    )

    val Medium = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )

    val SemiBold = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    )

    val Bold = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp // Cocok buat Header angka duit
    )
}