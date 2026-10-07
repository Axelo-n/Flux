package com.example.flux.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

data class UiPreferences(val language: String = "id", val theme: String = "teal")

object AppPreferences {
    val state = mutableStateOf(UiPreferences())
    private var storage: SharedPreferences? = null
    fun initialize(context: Context) {
        if (storage != null) return
        storage = context.applicationContext.getSharedPreferences("flux_appearance", Context.MODE_PRIVATE)
        val prefs = storage!!
        state.value = UiPreferences(prefs.getString("language", "id")?.takeIf { it in listOf("id", "en") } ?: "id", prefs.getString("theme", "teal")?.takeIf { it in listOf("white", "black", "teal", "violet", "luca") } ?: "teal")
    }
    fun language(context: Context, code: String) {
        require(code in listOf("id", "en"))
        initialize(context)
        storage!!.edit().putString("language", code).apply()
        state.value = state.value.copy(language = code)
    }
    fun theme(context: Context, key: String) {
        require(key in listOf("white", "black", "teal", "violet", "luca"))
        initialize(context)
        storage!!.edit().putString("theme", key).apply()
        state.value = state.value.copy(theme = key)
    }
    val locale: Locale get() = Locale.forLanguageTag(state.value.language)
}
