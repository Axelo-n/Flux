package com.example.flux

import com.example.flux.preferences.translate
import com.example.flux.ui.theme.fluxPalettes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class PresentationPreferencesTest {
    @Test fun translationsPreserveDynamicAmountsAndUserText() {
        assertEquals("Preferences", translate("Preferensi", "en"))
        assertEquals("Makanan & minuman", translate("Food and Beverages", "id"))
        assertEquals("From a budget of Rp 50.000", translate("Dari jatah Rp 50.000", "en"))
        assertEquals("3 transactions recorded", translate("3 transaksi tercatat", "en"))
        assertEquals("Delete 'warung budi'?", translate("Hapus 'warung budi'?", "en"))
        assertEquals("gorengan gembleng", translate("gorengan gembleng", "en"))
    }
    @Test fun allThemesKeepTextAndMoneyReadable() {
        fun contrast(a: Color, b: Color): Float {
            val first = a.luminance(); val second = b.luminance()
            return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
        }
        fluxPalettes.forEach { (key, palette) ->
            assertTrue("$key main text", contrast(palette.text, palette.background) >= 4.5f)
            assertTrue("$key field text", contrast(palette.text, palette.raised) >= 4.5f)
            assertTrue("$key supporting text", contrast(palette.muted, palette.raised) >= 4.5f)
            assertTrue("$key hero", contrast(palette.heroText, palette.heroEnd) >= 4.5f)
        }
    }
}
