package com.example.flux.ui.settings

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
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
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(!blocked, { blocked = false }, { Text(translate("Parser (${rules.count { !it.blocked }})")) }); FilterChip(blocked, { blocked = true }, { Text(translate("Blacklist (${rules.count { it.blocked }})")) }) } }
        item { OutlinedTextField(search, { search = it }, placeholder = { Text(translate("Cari keyword atau catatan")) }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), singleLine = true) }
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
        item { OutlinedButton({ test = !test }, modifier = Modifier.fillMaxWidth()) { Text(translate("${if (test) "Tutup" else "Uji"} contoh notifikasi")) } }
        if (test) item { Panel("Preview pencatatan") {
            OutlinedTextField(testTitle, { testTitle = it }, label = { Text(translate("Judul notifikasi")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(testText, { testText = it }, label = { Text(translate("Isi notifikasi")) }, modifier = Modifier.fillMaxWidth())
            val match = NotificationTransactionParser.blockedBy(testTitle, testText, rules)
            val parsed = NotificationTransactionParser.parse(testTitle, testText, rules)
            Text(when { match != null -> translate("Diabaikan oleh '${match.keyword}'"); parsed != null -> "${translate(if (parsed.isIncome) "Income" else "Expense")} · ${rupiah(parsed.amount.toLong())}\n${translate(parsed.category)}\n${parsed.note.ifBlank { translate("Catatan kosong") }}"; else -> translate("Belum dikenali; akan masuk log untuk diperiksa.") }, color = UITeal)
            Hint("Preview tidak menyimpan transaksi.")
        } }
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
            Box {
                Surface(onClick = { expanded = true }, color = UISurfaceRaised, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .35f))) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(translate("Kategori"), style = AppFont.Medium.copy(fontSize = 12.sp), color = UIGray)
                            Text(translate(category), style = AppFont.SemiBold.copy(fontSize = 17.sp), color = UIWhite)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = UITeal)
                    }
                }
                DropdownMenu(expanded, { expanded = false }) {
                    (transactionCategories.map { it.name } + "Income").forEach { name -> DropdownMenuItem({ Text(translate(name)) }, { category = name; expanded = false }) }
                }
            }
            FormField(note, { note = it }, "Catatan otomatis · opsional", placeholder = "Nasgor")
        }
        Hint(if (rule.blocked) "Contoh: blugether. Semua notifikasi yang mengandung keyword ini akan diabaikan." else "Kosongkan catatan untuk memakai nama merchant atau penerima dari notifikasi.")
        Action("Simpan", keyword.isNotBlank()) { save(rule.copy(keyword = keyword.trim(), targetCategory = category, targetNote = note.trim().ifBlank { null })) }
    }
}
