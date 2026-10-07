package com.example.flux.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.flux.preferences.AppPreferences

data class FluxPalette(val background: Color, val surface: Color, val raised: Color, val border: Color, val text: Color, val muted: Color, val accent: Color, val heroStart: Color, val heroEnd: Color, val heroText: Color, val light: Boolean = false)

val fluxPalettes = mapOf(
    "white" to FluxPalette(Color(0xFFFAFAFC), Color(0xFFFFFFFF), Color(0xFFF0F2F6), Color(0xFFDCE1E8), Color(0xFF19212C), Color(0xFF5A6777), Color(0xFF263342), Color(0xFFE8EDF3), Color(0xFFD8E0EA), Color(0xFF19212C), true),
    "black" to FluxPalette(Color(0xFF080808), Color(0xFF161616), Color(0xFF242424), Color(0xFF363636), Color(0xFFF2F2F2), Color(0xFFA5A5A5), Color(0xFFE0E0E0), Color(0xFF333333), Color(0xFF252525), Color(0xFFF2F2F2)),
    "teal" to FluxPalette(Color(0xFF0B0E14), Color(0xFF151C25), Color(0xFF1D2733), Color(0xFF2A3543), Color(0xFFF2F5F7), Color(0xFF9AAABD), Color(0xFF92E5D5), Color(0xFFA7EFDA), Color(0xFF80D7C9), Color(0xFF123A35)),
    "luca" to FluxPalette(Color(0xFFF0F3F8), Color(0xFFFFFFFF), Color(0xFFEFEFEF), Color(0xFFD9DCE3), Color(0xFF1C1E2A), Color(0xFF64656E), Color(0xFFFEBE50), Color(0xFFFFCF6A), Color(0xFFFEBE50), Color(0xFF1C1E2A), true),
    "violet" to FluxPalette(Color(0xFF15101F), Color(0xFF241C32), Color(0xFF322640), Color(0xFF4A395C), Color(0xFFF5F1FA), Color(0xFFB5A5C7), Color(0xFFC8AEFF), Color(0xFFDECFFD), Color(0xFFBFA2EB), Color(0xFF302044))
)
val currentPalette: FluxPalette get() = fluxPalettes.getValue(AppPreferences.state.value.theme)
val UIBlack: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF000000) else currentPalette.background
val UIWhite: Color get() = currentPalette.text
val UIBackground: Color get() = currentPalette.background
val UISurface: Color get() = currentPalette.surface
val UISurfaceRaised: Color get() = currentPalette.raised
val UIBorder: Color get() = currentPalette.border
// Amber is a fill in Luca; charcoal keeps labels readable on its light surfaces.
val UITeal: Color get() = if (AppPreferences.state.value.theme == "luca") currentPalette.text else currentPalette.accent
val UIAccentFill: Color get() = currentPalette.accent
val UIOnAccent: Color get() = if (AppPreferences.state.value.theme == "luca") currentPalette.text else currentPalette.background

data class FluxCardColors(val surface: Color, val text: Color, val muted: Color, val accent: Color, val border: Color, val negative: Color)

/** Selected summary cards get Luca's charcoal treatment; other themes keep their palette. */
fun cardColors(emphasized: Boolean = false): FluxCardColors =
    if (emphasized && AppPreferences.state.value.theme == "luca")
        FluxCardColors(currentPalette.text, Color.White, Color(0xFFCBCDD8), currentPalette.accent, Color(0xFF303341), Color(0xFFFF8B99))
    else FluxCardColors(UISurface, UIWhite, UIGray, UITeal, UIBorder, UIRed)
val UIBlue: Color get() = if (currentPalette.light) Color(0xFF586BA0) else Color(0xFFA9B9FF)
val UITertiary: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF475569) else currentPalette.muted
val UIGray: Color get() = currentPalette.muted
val UIGreen: Color get() = if (currentPalette.light) Color(0xFF167347) else Color(0xFF92E5B0)
val UIRed: Color get() = if (currentPalette.light) Color(0xFFBA334D) else Color(0xFFFF8B99)
val UIWarning: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFFFF7700) else if (currentPalette.light) Color(0xFF99500B) else Color(0xFFFFBE76)
val CatBlue: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF4F42E4) else Color(0xFF766CF0)
val CatOrange: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFFFF7700) else Color(0xFFD88938)
val CatPurple: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF6C5DD3) else Color(0xFF9A83DB)
val CatGreen: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF00CB6A) else Color(0xFF379B70)
val CatYellow: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFFF6B200) else Color(0xFFC2942B)
val CatCyan: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF0090FE) else Color(0xFF5097C5)
val CatGrey: Color get() = if (AppPreferences.state.value.theme == "teal") Color(0xFF94A3B8) else Color(0xFF8191A6)
val gradientBrush: Brush get() = Brush.linearGradient(listOf(currentPalette.heroStart, currentPalette.heroEnd))
