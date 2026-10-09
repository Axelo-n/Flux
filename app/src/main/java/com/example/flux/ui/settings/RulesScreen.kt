package com.example.flux.ui.settings

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.data.ParserRule
import com.example.flux.notification.NotificationTransactionParser
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.ui.transaction.transactionCategories
import com.example.flux.viewmodel.*

@Composable
fun RulesScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val rules by viewModel.parserRules.collectAsState()
    var search by remember { mutableStateOf("") }
    var blocked by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<ParserRule?>(null) }
    var remove by remember { mutableStateOf<ParserRule?>(null) }
    var testTitle by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("") }
    var test by remember { mutableStateOf(false) }
    val filtered = rules.filter { it.blocked == blocked && (it.keyword.contains(search, true) || it.targetNote.orEmpty().contains(search, true)) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PageHeader("Aturan otomatis", "Nama langganan jadi catatan. Notifikasi pengganggu berhenti di sini.", onBack)
        }
        item { FilterTabs(listOf("Parser (${rules.count { !it.blocked }})", "Blacklist (${rules.count { it.blocked }})"), if (blocked) 1 else 0) { blocked = it == 1 } }
        item { SearchField(search, { search = it }, "Cari keyword atau catatan") }
        item { Action(if (blocked) "+ Tambah blacklist" else "+ Tambah aturan") { editor = ParserRule(keyword = "", targetCategory = "Food and Beverages", blocked = blocked) } }
        item { Hint(if (blocked) "Blacklist diperiksa lebih dulu. Keyword cocok → notifikasi diabaikan." else "Pencocokan mengandung keyword, tanpa membedakan huruf besar/kecil. Aturan terbaru mendapat prioritas.") }
        if (filtered.isEmpty()) item { Panel { Hint(if (search.isBlank()) "Belum ada aturan. Mulai dari tempat yang paling sering lu pakai." else "Tidak ada aturan yang cocok.") } }
        items(filtered, key = { it.id }) { rule ->
            Surface(color = UISurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .65f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(rule.keyword, modifier = Modifier.weight(1f), style = AppFont.SemiBold.copy(fontSize = 18.sp), color = if (rule.enabled) UIWhite else UIGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Switch(rule.enabled, { viewModel.saveRule(rule.copy(enabled = it)) }, modifier = Modifier.semantics { contentDescription = "${translate("Aktifkan aturan")} ${rule.keyword}" })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(translate(if (rule.blocked) "Abaikan notifikasi" else rule.targetCategory), style = AppFont.Medium.copy(fontSize = 13.sp), color = if (rule.enabled) UITeal else UIGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!rule.blocked) Text(rule.targetNote?.takeIf { it.isNotBlank() } ?: translate("Catatan dari notifikasi"), style = AppFont.Regular.copy(fontSize = 13.sp), color = UIGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton({ editor = rule }) { Icon(Icons.Default.Edit, "Edit ${rule.keyword}", tint = UIGray, modifier = Modifier.size(18.dp)) }
                        IconButton({ remove = rule }) { Icon(Icons.Default.Delete, "Hapus ${rule.keyword}", tint = UIGray, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        item {
            Surface(onClick = { test = true }, color = UISurfaceRaised, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .5f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = UITeal.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Search, null, Modifier.padding(10.dp).size(20.dp), tint = UITeal) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(translate("Uji contoh notifikasi"), color = UIWhite, style = AppFont.SemiBold)
                        Text(translate("Coba parser tanpa membuat transaksi"), color = UIGray, style = AppFont.Regular.copy(fontSize = 12.sp))
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = UITeal)
                }
            }
        }
    }
    if (test) FluxDialog("Uji contoh notifikasi", "Coba parser tanpa membuat transaksi", Icons.Default.Search, { test = false }) {
        FormField(testTitle, { testTitle = it }, "Judul notifikasi", placeholder = "Yes, Transaksi QRIS Berhasil!")
        FormField(testText, { testText = it }, "Isi notifikasi", singleLine = false, placeholder = "Transaksi di warung budi Rp 5.000 berhasil")
        TextButton({ testTitle = "Yes, Transaksi QRIS Berhasil!"; testText = "Transaksi di warung budi Rp 5.000 berhasil" }) { Text(translate("Pakai contoh blu")) }
        val match = NotificationTransactionParser.blockedBy(testTitle, testText, rules)
        val parsed = NotificationTransactionParser.parse(testTitle, testText, rules, System.currentTimeMillis(), viewModel.config.value?.timezone ?: "Asia/Jakarta")
        val empty = testTitle.isBlank() && testText.isBlank()
        Surface(color = UISurfaceRaised, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .4f))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(translate("Preview pencatatan"), color = UIGray, style = AppFont.Medium.copy(fontSize = 12.sp))
                when {
                    empty -> Hint("Isi contoh untuk melihat hasil parser.")
                    match != null -> Text(translate("Diabaikan oleh '${match.keyword}'"), color = CatOrange, style = AppFont.SemiBold)
                    parsed != null -> {
                        Text(translate(if (parsed.isCashback) "Cashback" else if (parsed.isIncome) "Income" else "Expense"), color = UITeal, style = AppFont.SemiBold)
                        MoneyValue(parsed.amount.toLong(), UIWhite, 28.sp)
                        Text(translate(parsed.category), color = UIGray, style = AppFont.Medium)
                        Text(parsed.note.ifBlank { translate("Catatan kosong") }, color = UIWhite, style = AppFont.Regular)
                    }
                    else -> Hint("Belum dikenali; akan masuk log untuk diperiksa.")
                }
            }
        }
        Hint("Preview tidak menyimpan transaksi.")
    }
    editor?.let { rule -> RuleEditor(rule, { editor = null }) { viewModel.saveRule(it) { editor = null } } }
    remove?.let { rule -> AlertDialog(onDismissRequest = { remove = null }, title = { Text(translate("Hapus '${rule.keyword}'?")) }, text = { Text(translate("Transaksi yang sudah tercatat tetap tersimpan.")) }, confirmButton = { TextButton({ viewModel.deleteParserRule(rule); remove = null }) { Text(translate("Hapus")) } }, dismissButton = { TextButton({ remove = null }) { Text(translate("Batal")) } }) }
}

@Composable
private fun RuleEditor(rule: ParserRule, dismiss: () -> Unit, save: (ParserRule) -> Unit) {
    var keyword by remember(rule) { mutableStateOf(rule.keyword) }
    var note by remember(rule) { mutableStateOf(rule.targetNote.orEmpty()) }
    var category by remember(rule) { mutableStateOf(rule.targetCategory) }
    var expanded by remember { mutableStateOf(false) }
    FluxDialog(if (rule.blocked) "Blacklist keyword" else "Aturan pencatatan", if (rule.blocked) "Abaikan notifikasi yang mengandung keyword ini." else "Atur kategori dan catatan untuk transaksi langganan.", if (rule.blocked) Icons.Default.Delete else Icons.Default.Edit, dismiss) {
        FormField(keyword, { keyword = it }, "Keyword · contoh: nama penjual", placeholder = "warung budi")
        if (!rule.blocked) {
            com.example.flux.ui.transaction.CategorySelector(category, { category = it }, includeIncome = true)
            FormField(note, { note = it }, "Catatan otomatis · opsional", placeholder = "Nasgor")
        }
        Hint(if (rule.blocked) "Contoh: blugether. Semua notifikasi yang mengandung keyword ini akan diabaikan." else "Kosongkan catatan untuk memakai nama merchant atau penerima dari notifikasi.")
        Action("Simpan", keyword.isNotBlank()) { save(rule.copy(keyword = keyword.trim(), targetCategory = category, targetNote = note.trim().ifBlank { null })) }
    }
}
