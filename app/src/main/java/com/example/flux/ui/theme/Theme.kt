package com.example.flux.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.content.ContextWrapper
import android.content.res.Resources
import com.example.flux.preferences.AppPreferences
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
private fun fluxColorScheme() = (if (currentPalette.light) lightColorScheme() else darkColorScheme()).copy(
    primary = UITeal,        // Warna utama (Progress bar, Button aktif)
    onPrimary = UIBackground,// Warna teks di atas tombol primary
    secondary = UIBlue,      // Warna aksen kedua
    onSecondary = UIBackground,

    background = UIBackground, // 0xFF0B0E14
    onBackground = UIWhite,    // Teks utama

    surface = UISurface,       // 0xFF21262D (Buat Card/TopBar)
    onSurface = UIWhite,       // Teks di atas Card

    error = UIRed,
    onError = UIBackground,

    outline = UIGray,
    outlineVariant = UIBorder,
    surfaceVariant = UISurfaceRaised,
    onSurfaceVariant = UIGray,
    primaryContainer = UIAccentFill.copy(alpha = .15f),
    onPrimaryContainer = UITeal,
    secondaryContainer = UIAccentFill.copy(alpha = .14f),
    onSecondaryContainer = UITeal,
    surfaceTint = UITeal
)

@Composable
fun FluxTheme(
    // Kita hapus parameter dynamicColor biar konsisten pake warna kita
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    remember(context.applicationContext) { AppPreferences.initialize(context); true }
    val preferences = AppPreferences.state.value
    val localizedContext = remember(context, configuration, preferences.language) {
        val localized = context.createConfigurationContext(Configuration(configuration).apply { setLocale(AppPreferences.locale) })
        // Keep the Activity in the context chain for activity-result launchers and dialogs.
        object : ContextWrapper(context) { override fun getResources(): Resources = localized.resources }
    }
    val colorScheme = fluxColorScheme()
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Bikin status bar nyatu sama background
            window.statusBarColor = colorScheme.background.toArgb()
            // Icon status bar putih (karena background gelap)
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = currentPalette.light
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = currentPalette.light
        }
    }

    CompositionLocalProvider(LocalContext provides localizedContext, LocalConfiguration provides localizedContext.resources.configuration) { MaterialTheme(
        colorScheme = colorScheme,
        // Typography default Material tetep jalan, tapi kita bakal sering pake AppFont manual
        typography = Typography().let { base -> base.copy(
            headlineLarge = base.headlineLarge.copy(fontFamily = AfacadFamily),
            headlineMedium = base.headlineMedium.copy(fontFamily = AfacadFamily),
            headlineSmall = base.headlineSmall.copy(fontFamily = AfacadFamily),
            titleLarge = base.titleLarge.copy(fontFamily = AfacadFamily),
            titleMedium = base.titleMedium.copy(fontFamily = AfacadFamily),
            titleSmall = base.titleSmall.copy(fontFamily = AfacadFamily),
            bodyLarge = base.bodyLarge.copy(fontFamily = AfacadFamily),
            bodyMedium = base.bodyMedium.copy(fontFamily = AfacadFamily),
            bodySmall = base.bodySmall.copy(fontFamily = AfacadFamily),
            labelLarge = base.labelLarge.copy(fontFamily = AfacadFamily),
            labelMedium = base.labelMedium.copy(fontFamily = AfacadFamily),
            labelSmall = base.labelSmall.copy(fontFamily = AfacadFamily)
        ) },
        content = content
    ) }
}
