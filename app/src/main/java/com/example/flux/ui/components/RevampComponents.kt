package com.example.flux.ui.components

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.*
import java.time.*

@Composable
fun Page(title: String, subtitle: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 20.dp, bottom = if (onBack == null) 112.dp else 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader(title, subtitle, onBack)
        content()
    }
}

@Composable
fun Panel(title: String? = null, colors: FluxCardColors = cardColors(), content: @Composable ColumnScope.() -> Unit) {
    Surface(color = colors.surface, border = BorderStroke(1.dp, colors.border.copy(alpha = .6f)), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(translate(title), style = AppFont.SemiBold.copy(fontSize = 19.sp, color = colors.text))
            content()
        }
    }
}

@Composable
fun Hint(text: String, localized: Boolean = true, color: Color = UIGray) { Text(if (localized) translate(text) else text, style = AppFont.Regular.copy(fontSize = 14.sp, color = color)) }

@Composable
fun PageHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (onBack != null) TextButton(onBack, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(translate("Kembali"))
        }
        Text(translate(title), style = AppFont.Bold.copy(fontSize = 32.sp, letterSpacing = (-.5).sp, color = UIWhite))
        Hint(subtitle)
    }
}

/** Keeps the complete rupiah value readable even for unusually large balances. */
@Composable
fun MoneyValue(amount: Long, color: Color = UIWhite, fontSize: TextUnit = 28.sp, textAlign: TextAlign = TextAlign.Start) {
    val label = com.example.flux.viewmodel.rupiah(amount)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val style = AppFont.Bold.copy(fontSize = fontSize, letterSpacing = (-.4).sp)
        val naturalWidth = measurer.measure(label, style, softWrap = false).size.width
        val availableWidth = with(density) { maxWidth.toPx() }
        val scale = if (naturalWidth > 0) (availableWidth / naturalWidth).coerceAtMost(1f) else 1f
        Text(translate(label), modifier = Modifier.fillMaxWidth(), textAlign = textAlign, style = style.copy(fontSize = fontSize * scale, color = color), maxLines = 1, softWrap = false)
    }
}

@Composable
fun SectionHeading(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(translate(title), style = AppFont.SemiBold.copy(fontSize = 21.sp, color = UIWhite), modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun MonthSelector(label: String, previous: () -> Unit, next: () -> Unit) {
    Surface(color = UISurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(previous) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Bulan sebelumnya", tint = UIGray) }
            Text(translate(label), style = AppFont.SemiBold.copy(fontSize = 18.sp, color = UIWhite))
            IconButton(next) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Bulan berikutnya", tint = UIGray) }
        }
    }
}

@Composable
fun Action(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = UIAccentFill, contentColor = UIOnAccent)) {
        Text(translate(label), style = AppFont.Bold.copy(fontSize = 15.sp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInput(value: String, change: (String) -> Unit, valid: Boolean, earliest: LocalDate? = null, latest: LocalDate? = null, filled: Boolean = false) {
    var picker by remember { mutableStateOf(false) }
    if (filled) FormField(value, change, "Tanggal · YYYY-MM-DD", isError = !valid, trailingIcon = { TextButton({ picker = true }) { Text(translate("Pilih")) } })
    else OutlinedTextField(value, change, label = { Text(translate("Tanggal · YYYY-MM-DD")) }, isError = !valid, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { TextButton({ picker = true }) { Text(translate("Pilih")) } })
    if (picker) {
        val selected = runCatching { LocalDate.parse(value) }.getOrNull() ?: earliest ?: LocalDate.now()
        val dates = remember(earliest, latest) { object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return (earliest == null || day >= earliest) && (latest == null || day <= latest)
            }
        } }
        val state = rememberDatePickerState(initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = dates)
        DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = { TextButton({ state.selectedDateMillis?.let { change(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }; picker = false }, enabled = state.selectedDateMillis != null) { Text(translate("Pilih tanggal")) } }, dismissButton = { TextButton({ picker = false }) { Text(translate("Batal")) } }) { DatePicker(state) }
    }
}
