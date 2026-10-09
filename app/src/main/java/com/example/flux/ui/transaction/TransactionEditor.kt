package com.example.flux.ui.transaction

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.flux.model.*
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.viewmodel.*
import java.time.*

@Composable
fun TransactionEditor(viewModel: DashboardViewModel, transaction: Transaction?, onBack: () -> Unit) {
    val config by viewModel.config.collectAsState()
    val zone = ZoneId.of(config?.timezone ?: "Asia/Jakarta")
    var amount by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.originalAmount?.toLong()?.toString().orEmpty()) }
    var note by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.note.orEmpty()) }
    var category by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.category ?: MealCategories.at(System.currentTimeMillis(), zone.id)) }
    var income by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.isIncome ?: false) }
    var dateText by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.let { BudgetEngine.day(it.date, zone.id).toString() } ?: LocalDate.now(zone).toString()) }
    var refund by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.refund?.takeIf { it > 0 }?.toLong()?.toString().orEmpty()) }
    var refundNote by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.refundNote.orEmpty()) }
    var remove by remember { mutableStateOf(false) }
    var showRefund by rememberSaveable(transaction?.id) { mutableStateOf((transaction?.refund ?: 0.0) > 0) }
    val saving by viewModel.busy.collectAsState()
    val value = amount.toLongOrNull()
    val refundValue = if (income || refund.isBlank()) 0L else refund.toLongOrNull()
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val validDate = config != null && date != null && date.toEpochDay() >= config!!.startDay && date <= LocalDate.now(zone)
    val valid = value != null && value in 1..BudgetEngine.MAX_AMOUNT && refundValue != null && refundValue in 0..value && (refundValue == 0L || refundNote.isNotBlank()) && validDate
    Page(if (transaction == null) "Catat transaksi" else "Detail transaksi", "${if (transaction?.source == "blu") "Tercatat otomatis dari blu" else "Pencatatan manual"} · ${zone.id}", onBack) {
        IncomeExpenseToggle(income, onToggle = { income = it })
        Panel {
            FormField(amount, { if (it.all(Char::isDigit)) amount = it }, if (!income && (refundValue ?: 0) > 0) "Nominal sebelum refund" else "Nominal", money = true, prominent = true, placeholder = "0")
            Hint(if (income) "Pemasukan menambah saldo rekening." else "Pengeluaran memakai jatah hari transaksi, lalu extra jika perlu.")
        }
        Panel("Detail catatan") {
            DateInput(dateText, { dateText = it }, validDate, earliest = config?.let { LocalDate.ofEpochDay(it.startDay) }, latest = LocalDate.now(zone), filled = true)
            FormField(note, { note = it }, "Catatan · opsional", placeholder = "Contoh: nasgor malam", singleLine = false)
        }
        CategorySelector(category, { category = it })
        if (!income) Panel("Refund / cashback / cancel") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Hint("Ada refund atau cashback?")
                TextButton({ showRefund = !showRefund }) { Text(translate(if (showRefund) "Ringkas" else "Tambahkan refund / cashback")) }
            }
            if (showRefund) {
            Hint("Refund mengurangi pengeluaran pada tanggal asal. Saldo, budget, dan extra ikut dihitung ulang.")
            FormField(refund, { if (it.all(Char::isDigit)) refund = it }, "Total refund + cashback (Rp)", money = true)
            OutlinedButton({ refund = amount }, shape = RoundedCornerShape(14.dp)) { Text(translate("Refund penuh")) }
            if (!refund.isBlank() && (refundValue ?: 0) > 0) {
                FormField(refundNote, { refundNote = it }, "Alasan refund / cashback · wajib", singleLine = false)
                Text(translate("Pengeluaran efektif: ${rupiah((value ?: 0) - (refundValue ?: 0))}"), color = UITeal, style = AppFont.Bold)
            }
            }
        }
        Action(if (saving) "Menyimpan…" else "Simpan transaksi", valid && !saving) {
            val timestamp = if (transaction != null && BudgetEngine.day(transaction.date, zone.id) == date) transaction.date else date!!.atTime(LocalTime.now(zone)).atZone(zone).toInstant().toEpochMilli()
            viewModel.saveTransaction(transaction?.id ?: 0, value!!.toDouble(), note, category, income, timestamp, refundValue!!.toDouble(), if (refundValue > 0) refundNote else "", transaction?.source ?: "manual", onBack)
        }
        if (transaction != null) TextButton({ remove = true }) { Text(translate("Hapus transaksi"), color = UIRed) }
    }
    if (remove) AlertDialog(onDismissRequest = { remove = false }, title = { Text(translate("Hapus transaksi?")) }, text = { Text(translate("Saldo, budget, dan extra akan dihitung ulang. Gunakan refund jika transaksi dibatalkan agar riwayat tetap terlihat.")) }, confirmButton = { TextButton({ remove = false; viewModel.deleteTransaction(transaction!!.id, onBack) }) { Text(translate("Hapus")) } }, dismissButton = { TextButton({ remove = false }) { Text(translate("Batal")) } })
}
