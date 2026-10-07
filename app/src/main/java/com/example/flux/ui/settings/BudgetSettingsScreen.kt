package com.example.flux.ui.settings

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flux.model.BudgetEngine
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.viewmodel.*
import java.time.*

@Composable
fun BudgetSettingsScreen(viewModel: DashboardViewModel, onBack: (() -> Unit)? = null, restart: Boolean = false) {
    val config by viewModel.config.collectAsState()
    val policies by viewModel.policies.collectAsState()
    val setup = config == null || restart
    var timezone by remember(config, restart) { mutableStateOf(if (restart) "Asia/Jakarta" else config?.timezone ?: "Asia/Jakarta") }
    val initial = policies.lastOrNull()?.weeklyAmounts() ?: List(7) { 50_000L }
    var amounts by remember(policies, restart) { mutableStateOf(if (restart) List(7) { "50000" } else initial.map { it.toString() }) }
    var balance by remember { mutableStateOf("") }
    var effective by remember(config) { mutableStateOf(LocalDate.now(ZoneId.of(config?.timezone ?: "Asia/Jakarta")).plusDays(1).toString()) }
    var confirm by remember { mutableStateOf(false) }
    var customZone by remember { mutableStateOf(false) }
    val saving by viewModel.busy.collectAsState()
    val values = amounts.map { it.toLongOrNull() }
    val valid = values.all { it != null && it in 0..BudgetEngine.MAX_AMOUNT }
    val zoneValid = runCatching { ZoneId.of(timezone) }.isSuccess
    val date = runCatching { LocalDate.parse(effective) }.getOrNull()
    val today = LocalDate.now(ZoneId.of(config?.timezone ?: "Asia/Jakarta"))
    val dateValid = date != null && date > today
    Page(if (setup) "Mulai lebih tenang." else "Budget harian", if (setup) "Satu rekening blu. Jatah yang jelas. Sisa yang jadi extra." else "Atur jatah tiap hari tanpa mengubah perhitungan masa lalu.", onBack) {
        if (setup) Panel("01 · Saldo awal") {
            FormField(balance, { if (it.all(Char::isDigit)) balance = it }, "Saldo blu saat ini (Rp)", money = true, prominent = true, placeholder = "0")
            Hint("Isi sesuai saldo rekening sekarang. Extra mulai dari Rp0, dan hari pertama mendapat jatah penuh.")
            HorizontalDivider(color = UIBorder)
            Text(translate("Pergantian hari"), style = AppFont.SemiBold, color = UIWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Asia/Jakarta" to "WIB", "Asia/Makassar" to "WITA", "Asia/Jayapura" to "WIT").forEach { (zone, label) -> FilterChip(timezone == zone, { timezone = zone }, { Text(translate(label)) }) }
            }
            TextButton({ customZone = !customZone }) { Text(translate(if (customZone) "Tutup zona waktu lain" else "Zona waktu lain")) }
            if (customZone) FormField(timezone, { timezone = it.trim() }, "Zona waktu", isError = !zoneValid)
            Hint("${timezone} · Zona waktu dikunci selama periode pencatatan ini.")
        }
        Panel(if (setup) "02 · Jatah harian" else "Jatah harian") {
            Hint("Bisa berbeda tiap hari. Isi 0 untuk hari tanpa jatah.")
            val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
            TextButton({ amounts = List(7) { amounts.first() } }, enabled = values.first()?.let { it in 0..BudgetEngine.MAX_AMOUNT } == true) { Text(translate("Samakan semua dengan Senin")) }
            BoxWithConstraints {
                val columns = if (maxWidth >= 280.dp && LocalDensity.current.fontScale < 1.3f) 2 else 1
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    days.indices.toList().chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { index -> FormField(amounts[index], { input -> if (input.all(Char::isDigit)) amounts = amounts.toMutableList().apply { this[index] = input } }, days[index], modifier = Modifier.weight(1f), money = true, isError = amounts[index].isNotBlank() && (values[index] == null || values[index]!! > BudgetEngine.MAX_AMOUNT)) }
                        }
                    }
                }
            }
            HorizontalDivider(color = UIBorder)
            Hint("TOTAL JATAH SEMINGGU")
            if (valid) MoneyValue(values.sumOf { it!! }, UITeal, 24.sp)
            else Hint("Lengkapi nominal yang valid untuk melihat total.")
            Hint("Sisa jatah masuk extra setelah hari berakhir.")
        }
        if (!setup) Panel("Mulai berlaku") {
            DateInput(effective, { effective = it }, valid = dateValid, earliest = today.plusDays(1), filled = true)
            Hint("Paling cepat besok. Jadwal pada tanggal yang sama diperbarui selama belum berlaku. Budget hari sebelumnya tetap sama.")
        }
        Action(if (saving) "Menyimpan…" else if (setup) "Mulai pencatatan baru" else "Jadwalkan budget", !saving && valid && if (setup) zoneValid && balance.toLongOrNull()?.let { it in 0..BudgetEngine.MAX_AMOUNT } == true else dateValid) {
            if (setup) confirm = true else {
                viewModel.saveBudget(date!!, values.map { it!! }) { onBack?.invoke() }
            }
        }
        if (!setup) Panel("Riwayat kebijakan") {
            policies.reversed().forEach { policy ->
                Text(translate("Mulai ${LocalDate.ofEpochDay(policy.effectiveDay)}"), style = AppFont.SemiBold, color = UITeal)
                Hint(policy.weeklyAmounts().mapIndexed { i, amount -> "${listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")[i]} ${rupiah(amount)}" }.joinToString(" · "))
                if (policy.effectiveDay > today.toEpochDay()) Row {
                    TextButton({ effective = LocalDate.ofEpochDay(policy.effectiveDay).toString(); amounts = policy.weeklyAmounts().map { it.toString() } }) { Text(translate("Edit jadwal")) }
                    TextButton({ viewModel.cancelBudget(policy.effectiveDay) }, enabled = !saving) { Text(translate("Batalkan"), color = UIRed) }
                }
                HorizontalDivider(color = UIGray.copy(alpha = .15f))
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(translate("Mulai dari nol?")) }, text = { Text(translate("Semua transaksi, aturan, koreksi, dan log lama akan dihapus. Saldo awal ${rupiah(balance.toLongOrNull() ?: 0)}; extra Rp0. Backup dulu jika masih dibutuhkan.")) }, confirmButton = {
        TextButton(onClick = { confirm = false; viewModel.startSystem(balance.toLong(), timezone, values.map { it!! }) { onBack?.invoke() } }) { Text(translate("Hapus & mulai")) }
    }, dismissButton = { TextButton({ confirm = false }) { Text(translate("Batal")) } })
}
