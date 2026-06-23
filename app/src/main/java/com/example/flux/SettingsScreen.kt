package com.example.flux

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.tooling.preview.Preview
import com.example.flux.ui.theme.*
import kotlinx.coroutines.launch

// ==========================================
// 1. HOST COMPOSABLE (LOGIC & STATE MANAGER)
// ==========================================
@Composable
fun SettingsScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // --- STATE ---
    // 1. Ambil Rules dari Database (Live Update)
    val rules by viewModel.parserRules.collectAsState()

    // 2. Logic Cek Service Notification
    fun checkNotificationServiceAccess(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }
    var isServiceActive by remember { mutableStateOf(checkNotificationServiceAccess()) }

    // 3. Logic Cek Battery Optimization (true = exempted = bagus)
    fun checkBatteryOptimizationExempt(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }
    var isBatteryExempt by remember { mutableStateOf(checkBatteryOptimizationExempt()) }

    // Auto-refresh status pas balik ke aplikasi
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceActive = checkNotificationServiceAccess()
                isBatteryExempt = checkBatteryOptimizationExempt()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- LAUNCHERS (Permission & File System) ---

    // A. Launcher Izin Notifikasi (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showDummyNotification(context)
        else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    // B. Launcher EXPORT (Backup Data)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val jsonString = viewModel.createBackupJson()
                try {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // C. Launcher IMPORT (Restore Data)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonString = inputStream?.bufferedReader().use { reader -> reader?.readText() }

                if (jsonString != null) {
                    viewModel.restoreFromBackup(
                        jsonString = jsonString,
                        onSuccess = { Toast.makeText(context, "Data restored! Restarting recommended.", Toast.LENGTH_LONG).show() },
                        onError = { Toast.makeText(context, "Invalid backup file.", Toast.LENGTH_SHORT).show() }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- PANGGIL UI CONTENT ---
    SettingsScreenContent(
        isServiceActive = isServiceActive,
        isBatteryExempt = isBatteryExempt,
        parserRules = rules,
        // Actions
        onToggleService = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        },
        onFixBatteryOptimization = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        },
        onTestNotification = {
            if (Build.VERSION.SDK_INT >= 33) {
                val perm = android.Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                    showDummyNotification(context)
                } else {
                    notificationPermissionLauncher.launch(perm)
                }
            } else {
                showDummyNotification(context)
            }
        },
        onInjectCurrentBalance = { viewModel.injectCurrentBalance(it) },
        onInjectExtraBalance = { viewModel.injectExtraBalance(it) },
        onAddRule = { k, c, n -> viewModel.addParserRule(k, c, n) },
        onDeleteRule = { rule -> viewModel.deleteParserRule(rule) },
        onExportData = {
            val fileName = "flux_backup_${System.currentTimeMillis()}.json"
            exportLauncher.launch(fileName)
        },
        onImportData = {
            importLauncher.launch("application/json")
        }
    )
}

// ==========================================
// 2. UI CONTENT (TAMPILAN MURNI)
// ==========================================
@Composable
fun SettingsScreenContent(
    isServiceActive: Boolean,
    isBatteryExempt: Boolean = true,
    parserRules: List<ParserRule> = emptyList(),
    onToggleService: () -> Unit,
    onFixBatteryOptimization: () -> Unit = {},
    onTestNotification: () -> Unit,
    onInjectCurrentBalance: (Double) -> Unit,
    onInjectExtraBalance: (Double) -> Unit,
    onAddRule: (String, String, String?) -> Unit,
    onDeleteRule: (ParserRule) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit
) {
    val scrollState = rememberScrollState()
    var injectionAmount by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- HEADER ---
        Column {
            Text("Settings", style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite))
            Text("Control center & Debugging", style = AppFont.Medium.copy(fontSize = 16.sp, color = UIGray))
        }

        // --- SECTION 1: GENERAL ---
        SectionLabel("GENERAL")

        // Listener Toggle Card
        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(if (isServiceActive) UITeal.copy(0.2f) else UIGray.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.flux_transparent),
                            contentDescription = null,
                            tint = if (isServiceActive) UITeal else UIGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Auto-Record", style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                        Text(
                            if (isServiceActive) "Running in background" else "Permission needed",
                            style = AppFont.Medium.copy(color = if (isServiceActive) UITeal else UIGray, fontSize = 12.sp)
                        )
                    }
                }
                Switch(
                    checked = isServiceActive,
                    onCheckedChange = { onToggleService() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = UIWhite, checkedTrackColor = UITeal,
                        uncheckedThumbColor = UIGray, uncheckedTrackColor = UIBlack
                    )
                )
            }
        }

        // Battery Optimization Card
        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .then(if (!isBatteryExempt) Modifier.clickable { onFixBatteryOptimization() } else Modifier)
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(if (isBatteryExempt) UITeal.copy(0.2f) else CatOrange.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.flux_transparent),
                            contentDescription = null,
                            tint = if (isBatteryExempt) UITeal else CatOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Battery Optimization", style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                        Text(
                            if (isBatteryExempt) "Disabled — listener runs freely" else "Enabled — tap to fix!",
                            style = AppFont.Medium.copy(
                                color = if (isBatteryExempt) UITeal else CatOrange,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                if (!isBatteryExempt) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CatOrange)
                }
            }
        }

        // Test Notification Card
        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .clickable { onTestNotification() }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(CatPurple.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = CatPurple)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Notification", style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                    Text("Simulate a budget alert", style = AppFont.Medium.copy(color = UIGray, fontSize = 12.sp))
                }
                Icon(painterResource(android.R.drawable.ic_media_play), null, tint = UIGray, modifier = Modifier.size(12.dp))
            }
        }

        // --- SECTION 2: DEBUG ZONE ---
        SectionLabel("DEBUG ZONE (MONEY INJECTOR)")

        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Manual Balance Injection", style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = injectionAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '-' || c == '.' }) injectionAmount = it },
                    label = { Text("Amount (Rp)") },
                    placeholder = { Text("Example: 50000 or -20000") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = UIWhite, unfocusedTextColor = UIWhite,
                        focusedBorderColor = UITeal, unfocusedBorderColor = UIGray.copy(0.5f),
                        focusedContainerColor = UIBlack.copy(0.3f), unfocusedContainerColor = UIBlack.copy(0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val amt = injectionAmount.toDoubleOrNull() ?: 0.0
                            if (amt != 0.0) {
                                onInjectCurrentBalance(amt)
                                Toast.makeText(context, "Current Balance updated", Toast.LENGTH_SHORT).show()
                                injectionAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UITeal),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Current", style = AppFont.Bold.copy(fontSize = 18.sp)) }

                    Button(
                        onClick = {
                            val amt = injectionAmount.toDoubleOrNull() ?: 0.0
                            if (amt != 0.0) {
                                onInjectExtraBalance(amt)
                                Toast.makeText(context, "Extra Balance updated", Toast.LENGTH_SHORT).show()
                                injectionAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UITeal),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Extra", style = AppFont.Bold.copy(fontSize = 18.sp)) }
                }
            }
        }

        // --- SECTION 3: SMART PARSER RULES ---
        SectionLabel("SMART PARSER RULES")

        // Add Button
        FluxCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { showAddDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(UITeal.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = UITeal, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Add New Rule", style = AppFont.Bold.copy(color = UIWhite, fontSize = 14.sp))
            }
        }

        // Rules List
        if (parserRules.isEmpty()) {
            Text(
                "No custom rules yet. Flux uses default logic.",
                style = AppFont.Regular.copy(color = UIGray.copy(0.5f), fontSize = 12.sp),
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
        } else {
            FluxCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    parserRules.forEachIndexed { index, rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.keyword, style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 14.sp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CatOrange))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(rule.targetCategory, style = AppFont.Medium.copy(color = UIGray, fontSize = 11.sp))
                                    if (!rule.targetNote.isNullOrEmpty()) {
                                        Text(" • ${rule.targetNote}", style = AppFont.Regular.copy(color = UIGray.copy(0.7f), fontSize = 11.sp), maxLines = 1)
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onDeleteRule(rule) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UIGray.copy(0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                        if (index < parserRules.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = UIGray.copy(0.15f))
                        }
                    }
                }
            }
        }

        // --- SECTION 4: DATA MANAGEMENT ---
        SectionLabel("DATA MANAGEMENT")

        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // 1. TOMBOL BACKUP (EXPORT)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportData() }
                        .padding(horizontal = 16.dp, vertical = 14.dp), // Padding konsisten
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Style (Mirip Add Rule)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UITeal.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = UITeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Backup Data",
                            style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 14.sp)
                        )
                        Text(
                            text = "Export to JSON file",
                            style = AppFont.Medium.copy(color = UIGray, fontSize = 12.sp)
                        )
                    }
                }

                // Garis Pemisah (Rapi, agak menjorok dikit)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    thickness = 0.5.dp,
                    color = UIGray.copy(0.15f)
                )

                // 2. TOMBOL RESTORE (IMPORT)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImportData() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Style (Warna Orange biar beda)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CatOrange.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown, // Atau KeyboardArrowDown
                            contentDescription = null,
                            tint = CatOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Restore Data",
                            style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 14.sp)
                        )
                        Text(
                            text = "Import from JSON file",
                            style = AppFont.Medium.copy(color = UIGray, fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(150.dp))
    }

    // --- POPUPS ---
    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { k, c, n ->
                onAddRule(k, c, n)
                showAddDialog = false
            }
        )
    }
}

// ==========================================
// 3. KOMPONEN PENDUKUNG (DIALOG & HELPER)
// ==========================================

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = AppFont.Bold.copy(fontSize = 12.sp, color = UIGray, letterSpacing = 1.sp),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@SuppressLint("MissingPermission")
fun showDummyNotification(context: Context) {
    val channelId = "flux_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Flux Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.flux_transparent)
        .setColor("#0B0E14".toColorInt())
        .setContentTitle("Flux Budget Alert")
        .setContentText("You've used 80% of your daily budget!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(1, notification)
}

// --- GLOW UP ADD RULE DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food and Beverages") }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf("Food and Beverages", "Transportation", "Groceries and Shopping", "Entertainment", "Account Transfer", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D24),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(UITeal.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.flux_transparent), null, tint = UITeal, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("New Rule", style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FluxInput(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = "Keyword",
                    placeholder = "e.g. Kopi Kenangan",
                    icon = Icons.Default.Search
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    FluxInput(
                        value = category, onValueChange = {}, label = "Auto-Category", placeholder = "Select Category",
                        icon = Icons.Default.List, readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = UIGray) }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                    DropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF252932)).width(260.dp)
                    ) {
                        categories.forEach { catName ->
                            DropdownMenuItem(
                                text = { Text(catName, style = AppFont.Medium.copy(color = UIWhite)) },
                                onClick = { category = catName; expanded = false },
                                colors = MenuDefaults.itemColors(textColor = UIWhite, leadingIconColor = UITeal)
                            )
                        }
                    }
                }
                FluxInput(
                    value = note,
                    onValueChange = { note = it },
                    label = "Auto-Note (Optional)",
                    placeholder = "e.g. Jajan Kopi",
                    icon = Icons.Default.Edit
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyword.isNotEmpty()) onSave(keyword, category, note.ifEmpty { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = UITeal, disabledContainerColor = UITeal.copy(0.5f)),
                shape = RoundedCornerShape(12.dp),
                enabled = keyword.isNotEmpty()
            ) { Text("Save Rule", style = AppFont.Bold.copy(color = UIBlack, fontSize = 14.sp)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = AppFont.Medium.copy(color = UIGray, fontSize = 14.sp)) }
        }
    )
}

// --- CUSTOM INPUT (BIAR GA NGULANG KODE) ---
@Composable
fun FluxInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text(label, style = AppFont.Regular.copy(fontSize = 12.sp)) },
        placeholder = { Text(placeholder, style = AppFont.Regular.copy(color = UIGray.copy(0.4f))) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = UIGray) },
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = UIWhite, unfocusedTextColor = UIWhite,
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = UITeal, unfocusedBorderColor = UIGray.copy(0.3f),
            focusedLabelColor = UITeal, unfocusedLabelColor = UIGray, cursorColor = UITeal
        ),
        textStyle = AppFont.Medium.copy(fontSize = 14.sp),
        modifier = modifier.fillMaxWidth()
    )
}