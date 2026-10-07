package com.example.flux.ui.settings

import com.example.flux.preferences.translate

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.flux.notification.FluxNotificationListenerService
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.viewmodel.*
import com.example.flux.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*

@Composable
fun SettingsScreen(viewModel: DashboardViewModel, onBudget: () -> Unit = {}, onRules: () -> Unit = {}, onRestart: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val health by FluxNotificationListenerService.health.collectAsState()
    val config by viewModel.config.collectAsState()
    val logs by viewModel.notifications.collectAsState()
    val adjustments by viewModel.adjustments.collectAsState()
    fun allowed(): Boolean = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.split(":")?.any { ComponentName.unflattenFromString(it)?.packageName == context.packageName } == true
    fun battery(): Boolean = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
    var access by remember { mutableStateOf(allowed()) }
    var exempt by remember { mutableStateOf(battery()) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf("saldo") }
    var amount by remember { mutableStateOf("") }
    var subtract by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showCorrection by remember { mutableStateOf(false) }
    var showAllLogs by remember { mutableStateOf(false) }
    var chooseLanguage by remember { mutableStateOf(false) }
    var chooseTheme by remember { mutableStateOf(false) }
    val preferences = AppPreferences.state.value
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { access = allowed(); exempt = battery() } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> Toast.makeText(context, translate(if (granted) "Izin notifikasi diberikan" else "Izin notifikasi belum diberikan"), Toast.LENGTH_SHORT).show() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = viewModel.createBackupJson()
                withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(json.toByteArray(Charsets.UTF_8)) } }
                Toast.makeText(context, translate("Backup tersimpan"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, translate("Backup gagal disimpan"), Toast.LENGTH_LONG).show() }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            try {
                pendingImport = withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader().use { reader ->
                    val buffer = CharArray(1024 * 1024 * 5)
                    var length = 0
                    while (length < buffer.size) { val count = reader.read(buffer, length, buffer.size - length); if (count < 0) break; length += count }
                    require(reader.read() == -1) { "Backup terlalu besar" }
                    String(buffer, 0, length)
                } }
            } catch (e: Exception) { Toast.makeText(context, translate("Backup tidak bisa dibaca (maksimal 5 MB)"), Toast.LENGTH_LONG).show() }
        }
    }
    Page("Sistem", "Atur kebiasaan, pencatatan, dan data lu di satu tempat.") {
        Panel("Preferensi") {
            SettingsLink("Budget harian", "Sesuaikan jatah setiap hari", Icons.Default.Settings, onBudget)
            HorizontalDivider(color = UIBorder)
            SettingsLink("Aturan otomatis & blacklist", "Kategori otomatis, tanpa notifikasi pengganggu", Icons.Default.List, onRules)
            HorizontalDivider(color = UIBorder)
            SettingsLink("Bahasa", if (preferences.language == "id") "Indonesia" else "English", FluxIcons.Language, { chooseLanguage = true })
            SettingsLink("Tema", mapOf("white" to "Putih", "black" to "Hitam", "teal" to "Teal", "violet" to "Violet", "luca" to "Luca").getValue(preferences.theme), FluxIcons.Theme, { chooseTheme = true })
            Hint("blu · ${config?.timezone ?: "WIB"} · mulai ${config?.let { LocalDate.ofEpochDay(it.startDay) } ?: "—"}")
        }
        Panel("Pencatatan blu") {
            Text(translate(if (access && health.connected) "● Terhubung" else if (access) "● Belum tersambung" else "● Akses belum diberikan"), color = if (access && health.connected) UITeal else CatOrange, style = AppFont.Bold)
            Hint("Status ini mengikuti koneksi listener, bukan hanya izin Android.")
            health.error?.let { Hint(it) }
            if (health.lastEvent > 0) Hint("Notifikasi terakhir: ${Instant.ofEpochMilli(health.lastEvent).atZone(ZoneId.of(config?.timezone ?: "Asia/Jakarta")).toLocalDateTime()}")
            OutlinedButton({ context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text(translate("Atur akses notifikasi")) }
            OutlinedButton({ FluxNotificationListenerService.reconnect(context) }, enabled = access, modifier = Modifier.fillMaxWidth()) { Text(translate("Hubungkan ulang")) }
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) OutlinedButton({ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text(translate("Izinkan notifikasi layanan")) }
            if (!exempt) OutlinedButton({
                runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))) }.onFailure { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }, modifier = Modifier.fillMaxWidth()) { Text(translate("Izinkan berjalan di background")) }
            Hint("Saat tersambung ulang, Flux memeriksa notifikasi blu yang masih tersedia. Notifikasi yang sudah hilang perlu dicatat manual.")
        }
        Panel("Koreksi manual") {
            Hint("Sesuaikan saldo atau extra jika ada selisih catatan.")
            OutlinedButton({ showCorrection = !showCorrection }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(translate(if (showCorrection) "Tutup koreksi" else "Buat koreksi")) }
            if (showCorrection) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("saldo", "extra").forEach { choice -> FilterChip(target == choice, { target = choice }, { Text(translate(choice.replaceFirstChar(Char::uppercase))) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(!subtract, { subtract = false }, { Text(translate("Tambah")) }); FilterChip(subtract, { subtract = true }, { Text(translate("Kurangi")) }) }
            OutlinedTextField(amount, { if (it.all(Char::isDigit)) amount = it }, label = { Text(translate("Nominal (Rp)")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text(translate("Alasan koreksi")) }, modifier = Modifier.fillMaxWidth())
            Hint("Koreksi saldo hanya mengubah saldo. Koreksi extra hanya mengubah extra, bukan pemasukan atau pengeluaran.")
            Action("Simpan koreksi", amount.toLongOrNull()?.let { it in 1..com.example.flux.model.BudgetEngine.MAX_AMOUNT } == true && note.isNotBlank()) { viewModel.adjust(target, amount.toLong() * if (subtract) -1 else 1, note) { amount = ""; note = "" } }
            }
            adjustments.take(10).forEach { row ->
                Text(translate("${row.target} · ${rupiah(row.amount)}"), color = UIWhite, style = AppFont.SemiBold)
                Hint(row.note, localized = false)
                TextButton({ viewModel.deleteAdjustment(row) }) { Text(translate("Batalkan koreksi"), color = UIRed) }
            }
        }
        Panel("Backup & data") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton({ export.launch("flux_${LocalDate.now()}.json") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(translate("Ekspor backup")) }
                OutlinedButton({ import.launch("application/json") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(translate("Restore backup")) }
            }
            TextButton(onRestart) { Text(translate("Mulai ulang dari nol"), color = UIRed) }
            Hint("Backup menyimpan transaksi, aturan, budget, dan koreksi. Restore mengganti data setelah validasi; log notifikasi tidak ikut diekspor.")
        }
        Panel("Aktivitas notifikasi · 100 terbaru") {
            if (logs.isEmpty()) Hint("Belum ada notifikasi blu sejak sistem dimulai.")
            (if (showAllLogs) logs else logs.take(3)).forEach { log ->
                Text(translate(log.status), color = if (log.status == "Tercatat") UITeal else CatOrange, style = AppFont.SemiBold)
                Text(log.title, color = UIWhite)
                Hint(log.text, localized = false)
                Hint(log.detail)
                Hint(Instant.ofEpochMilli(log.postedAt).atZone(ZoneId.of(config?.timezone ?: "Asia/Jakarta")).toLocalDateTime().toString())
                HorizontalDivider(color = UIGray.copy(alpha = .15f))
            }
            if (logs.size > 3) TextButton({ showAllLogs = !showAllLogs }) { Text(translate(if (showAllLogs) "Ringkas aktivitas" else "Lihat semua ${logs.size} notifikasi")) }
        }
    }
    if (pendingImport != null) AlertDialog(onDismissRequest = { pendingImport = null }, title = { Text(translate("Ganti data dengan backup?")) }, text = { Text(translate("Data sekarang akan diganti seluruhnya jika backup valid. Backup versi lama Flux tidak didukung.")) }, confirmButton = { TextButton({ val json = pendingImport!!; pendingImport = null; viewModel.restoreFromBackup(json, {}, {}) }) { Text(translate("Restore")) } }, dismissButton = { TextButton({ pendingImport = null }) { Text(translate("Batal")) } })
    if (chooseLanguage) FluxDialog("Pilih bahasa", "Bahasa tampilan aplikasi", FluxIcons.Language, { chooseLanguage = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("id" to "Indonesia", "en" to "English").forEach { (code, name) ->
                val selected = preferences.language == code
                Surface(onClick = { AppPreferences.language(context, code); chooseLanguage = false }, color = if (selected) UITeal.copy(alpha = .1f) else UISurfaceRaised, border = BorderStroke(1.dp, if (selected) UITeal.copy(alpha = .65f) else UIBorder), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(code.uppercase(), style = AppFont.Bold.copy(fontSize = 14.sp), color = UITeal)
                        Text(name, style = AppFont.SemiBold.copy(fontSize = 18.sp), color = UIWhite, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Default.Check, null, tint = UITeal, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
    if (chooseTheme) FluxDialog("Pilih tema", "Tampilan seluruh aplikasi", FluxIcons.Theme, { chooseTheme = false }) {
        listOf("white" to "Putih", "black" to "Hitam", "teal" to "Teal", "violet" to "Violet", "luca" to "Luca").chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (key, name) ->
                    val palette = fluxPalettes.getValue(key)
                    val selected = preferences.theme == key
                    Surface(onClick = { AppPreferences.theme(context, key); chooseTheme = false }, modifier = Modifier.weight(1f), color = palette.background, border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.accent else palette.border), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = palette.surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, palette.border)) {
                                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(color = palette.accent, shape = RoundedCornerShape(4.dp), modifier = Modifier.width(28.dp).height(5.dp)) {}
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Surface(color = palette.raised, shape = RoundedCornerShape(5.dp), modifier = Modifier.weight(1f).height(24.dp)) {}
                                        Surface(color = palette.accent.copy(alpha = .25f), shape = RoundedCornerShape(5.dp), modifier = Modifier.weight(1f).height(24.dp)) {}
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(translate(name), color = palette.text, style = AppFont.SemiBold.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
                                if (selected) Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = UISurface, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = UITeal.copy(alpha = .1f), shape = RoundedCornerShape(12.dp)) { Icon(icon, null, Modifier.padding(10.dp).size(20.dp), tint = UITeal) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(translate(title), style = AppFont.SemiBold, color = UIWhite); Hint(subtitle) }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = UIGray)
        }
    }
}
