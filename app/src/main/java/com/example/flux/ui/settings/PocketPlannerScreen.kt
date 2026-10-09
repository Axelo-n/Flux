package com.example.flux.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.data.MonthlyPocket
import com.example.flux.model.*
import com.example.flux.preferences.AppPreferences
import com.example.flux.preferences.translate
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.viewmodel.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun PocketPlannerScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val policies by viewModel.policies.collectAsState()
    val fixed by viewModel.monthlyPockets.collectAsState()
    val config by viewModel.config.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now(ZoneId.of(config?.timezone ?: "Asia/Jakarta"))) }
    val weeks = remember(month, policies) { PocketPlanner.plan(month, policies) }
    var selectedWeek by remember(month) { mutableStateOf<PocketWeek?>(null) }
    var selectedManual by remember { mutableStateOf<MonthlyPocket?>(null) }
    var editor by remember { mutableStateOf<MonthlyPocket?>(null) }
    var remove by remember { mutableStateOf<MonthlyPocket?>(null) }
    val dayFormat = DateTimeFormatter.ofPattern("d MMM", AppPreferences.locale)
    val monthLabel = month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", AppPreferences.locale))
    val weeklyTotal = weeks.sumOf { it.total }
    val manualTotal = fixed.sumOf { it.amount }
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(2) }) { PageHeader("Budget pocket", "Semua alokasi bulanan, dalam satu tempat.", onBack) }
        item(span = { GridItemSpan(2) }) { MonthSelector(monthLabel, { month = month.minusMonths(1) }, { month = month.plusMonths(1) }) }
        item(span = { GridItemSpan(2) }) {
            Surface(color = UIAccentFill, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(translate("DANA BULAN INI"), color = UIOnAccent, style = AppFont.SemiBold.copy(fontSize = 11.sp, letterSpacing = 1.sp))
                    MoneyValue(weeklyTotal + manualTotal, UIOnAccent, 32.sp)
                    HorizontalDivider(color = UIOnAccent.copy(alpha = .15f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(translate("Mingguan"), color = UIOnAccent.copy(alpha = .75f), style = AppFont.Medium.copy(fontSize = 11.sp))
                            MoneyValue(weeklyTotal, UIOnAccent, 16.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(translate("Bulanan tetap"), color = UIOnAccent.copy(alpha = .75f), style = AppFont.Medium.copy(fontSize = 11.sp))
                            MoneyValue(manualTotal, UIOnAccent, 16.sp)
                        }
                    }
                }
            }
        }
        item(span = { GridItemSpan(2) }) { SectionHeading("Pocket mingguan") }
        items(weeks, key = { "week-${month}-${it.number}" }) { week ->
            CompactPocketCard("${translate("Pocket minggu")} ${week.number}", "${week.days.first().format(dayFormat)} – ${week.days.last().format(dayFormat)}",
                week.total, week.number.toString().padStart(2, '0'), week.number % 2 == 1) { selectedWeek = week }
        }
        item(span = { GridItemSpan(2) }) {
            SectionHeading("Pocket bulanan") {
                FilledTonalButton({ editor = MonthlyPocket(name = "", amount = 0) }, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(translate("Tambah"), style = AppFont.SemiBold.copy(fontSize = 12.sp))
                }
            }
        }
        if (fixed.isEmpty()) item(span = { GridItemSpan(2) }) { Hint("Tambahkan kos, tabungan, atau alokasi lain dengan nominal tetap setiap bulan.") }
        items(fixed, key = { "fixed-${it.id}" }) { pocket ->
            CompactPocketCard(pocket.name, translate("Tetap tiap bulan"), pocket.amount, "Rp", false) { selectedManual = pocket }
        }
        item(span = { GridItemSpan(2) }) { Hint("Ketuk kartu untuk rincian. Ini kalkulator alokasi; tidak membuat transaksi atau memindahkan uang.") }
    }
    selectedWeek?.let { week ->
        FluxDialog("${translate("Pocket minggu")} ${week.number}", "${week.days.first().format(dayFormat)} – ${week.days.last().format(dayFormat)} · ${week.days.size} ${translate("hari")}", Icons.Default.Edit, { selectedWeek = null }) {
            MoneyValue(week.total, UITeal, 32.sp)
            Text(translate("Rincian harian"), color = UIWhite, style = AppFont.SemiBold)
            week.days.zip(week.amounts).forEach { (day, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(day.format(DateTimeFormatter.ofPattern("EEEE, d MMM", AppPreferences.locale)), Modifier.weight(1f), color = UIGray, style = AppFont.Regular.copy(fontSize = 13.sp))
                    Text(rupiah(amount), color = UIWhite, style = AppFont.SemiBold.copy(fontSize = 13.sp))
                }
            }
            Hint("Nominal mengikuti budget harian. Tanggal sebelum budget pertama memakai pola pertama yang tersedia.")
        }
    }
    selectedManual?.let { pocket ->
        FluxDialog(pocket.name, monthLabel, Icons.Default.Edit, { selectedManual = null }) {
            MoneyValue(pocket.amount, UITeal, 32.sp)
            Hint("Nominal tetap setiap bulan, sampai lu ubah atau hapus.")
            Action("Edit pocket", !busy) { selectedManual = null; editor = pocket }
            TextButton({ selectedManual = null; remove = pocket }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = UIRed); Spacer(Modifier.width(8.dp)); Text(translate("Hapus pocket"), color = UIRed)
            }
        }
    }
    editor?.let { pocket ->
        var name by remember(pocket) { mutableStateOf(pocket.name) }
        var amount by remember(pocket) { mutableStateOf(if (pocket.amount > 0) pocket.amount.toString() else "") }
        val valid = name.isNotBlank() && amount.toLongOrNull()?.let { it in 1..BudgetEngine.MAX_AMOUNT } == true
        FluxDialog(if (pocket.id == 0) "Tambah pocket bulanan" else "Edit pocket", "Nominal tetap, otomatis masuk pembagian setiap bulan.", Icons.Default.Edit, { editor = null }) {
            FormField(name, { name = it.take(80) }, "Nama pocket", placeholder = "Uang kos")
            FormField(amount, { if (it.all(Char::isDigit) && it.length <= 15) amount = it }, "Nominal per bulan", money = true)
            Hint("Hanya alokasi; saldo dan budget harian tetap sama.")
            Action("Simpan pocket", valid && !busy) { viewModel.saveMonthlyPocket(pocket.copy(name = name.trim(), amount = amount.toLong())) { editor = null } }
        }
    }
    remove?.let { pocket ->
        FluxDialog("Hapus pocket?", pocket.name, Icons.Default.Delete, { remove = null }) {
            Hint("Pocket ini akan dihapus dari semua pembagian bulanan. Transaksi tetap tersimpan.")
            Action("Hapus pocket", !busy) { viewModel.deleteMonthlyPocket(pocket) { remove = null } }
        }
    }
}

@Composable
private fun CompactPocketCard(name: String, period: String, amount: Long, badge: String, emphasized: Boolean, click: () -> Unit) {
    val colors = cardColors(emphasized)
    Surface(onClick = click, color = colors.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, colors.border.copy(alpha = .6f)),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${translate("Detail pocket")} $name" }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = colors.accent.copy(alpha = .12f), shape = RoundedCornerShape(10.dp)) {
                    Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = colors.accent, style = AppFont.Bold.copy(fontSize = 11.sp))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.muted, modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, color = colors.text, style = AppFont.SemiBold.copy(fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(period, color = colors.muted, style = AppFont.Regular.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MoneyValue(amount, colors.text, 21.sp)
        }
    }
}
