package com.example.flux

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.flux.ui.theme.*
import androidx.core.graphics.toColorInt
import android.provider.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var injectionAmount by remember { mutableStateOf("") }

    val parserRules by viewModel.parserRules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    fun checkNotificationServiceAccess(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }

    var isServiceActive by remember { mutableStateOf(checkNotificationServiceAccess()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceActive = checkNotificationServiceAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }

    var isAccessGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }

    // Launcher Izin Notifikasi
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showDummyNotification(context)
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- HEADER ---
        Column {
            Text(
                text = "Settings",
                style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite)
            )
            Text(
                text = "Control center & Debugging",
                style = AppFont.Medium.copy(fontSize = 16.sp, color = UIGray)
            )
        }

        // --- SECTION 1: GENERAL ---
        SectionLabel("GENERAL")

        // Voice Listener Card
        // --- 1. LISTENER TOGGLE ---
        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon Status
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isServiceActive) UITeal.copy(alpha = 0.2f) else UIGray.copy(alpha = 0.1f)),
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
                            style = AppFont.Medium.copy(
                                color = if (isServiceActive) UITeal else UIGray,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isServiceActive,
                    onCheckedChange = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = UIWhite,
                        checkedTrackColor = UITeal,
                        uncheckedThumbColor = UIGray,
                        uncheckedTrackColor = UIBlack
                    )
                )
            }
        }

        DisposableEffect(Unit) {
            val onResume = { isAccessGranted = isNotificationListenerEnabled(context) }
            onDispose { }
        }

        // Notification Test Card
        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (Build.VERSION.SDK_INT >= 33) {
                            val permission = android.Manifest.permission.POST_NOTIFICATIONS
                            val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                            if (isGranted) showDummyNotification(context) else notificationPermissionLauncher.launch(permission)
                        } else {
                            showDummyNotification(context)
                        }
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CatPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = CatPurple)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Notification", style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                    Text("Simulate a budget alert", style = AppFont.Medium.copy(color = UIGray, fontSize = 12.sp))
                }
                // Arrow icon indicator
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_play),
                    contentDescription = null,
                    tint = UIGray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // --- SECTION 2: DEBUG ZONE ---
        SectionLabel("DEBUG ZONE (MONEY INJECTOR)")

        FluxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Manual Balance Injection", style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = injectionAmount,
                    onValueChange = { input ->
                        if (input.all { char -> char.isDigit() || char == '-' || char == '.' }) {
                            injectionAmount = input
                        }
                    },
                    label = { Text("Amount (Rp)") },
                    placeholder = { Text("Example: 50000 or -20000") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp), // Lebih rounded
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = UIWhite,
                        unfocusedTextColor = UIWhite,
                        focusedBorderColor = UITeal,
                        unfocusedBorderColor = UIGray.copy(alpha = 0.5f),
                        focusedContainerColor = UIBlack.copy(alpha = 0.3f),
                        unfocusedContainerColor = UIBlack.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Tombol Current Balance
                    Button(
                        onClick = {
                            val amt = injectionAmount.toDoubleOrNull() ?: 0.0
                            if (amt != 0.0) {
                                viewModel.injectCurrentBalance(amt)
                                val msg = if (amt > 0) "Current Balance +$amt" else "Current Balance $amt"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                injectionAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UITeal),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", style = AppFont.Bold.copy(fontSize = 18.sp))
                        }
                    }

                    // Tombol Extra Balance
                    Button(
                        onClick = {
                            val amt = injectionAmount.toDoubleOrNull() ?: 0.0
                            if (amt != 0.0) {
                                viewModel.injectExtraBalance(amt)
                                val msg = if (amt > 0) "Extra Balance +$amt" else "Extra Balance $amt"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                injectionAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UITeal),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Extra", style = AppFont.Bold.copy(fontSize = 18.sp))
                        }
                    }
                }
            }
        }

        // --- SMART PARSER RULES ---
        SectionLabel("SMART PARSER RULES")

        // 1. Tombol Add (Tetap dipisah biar gampang diakses)
        FluxCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp) // Jarak dikit ke list bawahnya
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

        // 2. LIST RULES (Disatukan dalam 1 Card)
        if (parserRules.isEmpty()) {
            // State Kosong (Desain Minimalis)
            Text(
                "No custom rules yet. Flux uses default logic.",
                style = AppFont.Regular.copy(color = UIGray.copy(0.5f), fontSize = 12.sp),
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
        } else {
            FluxCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    parserRules.forEachIndexed { index, rule ->
                        // --- ITEM RULE ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp), // Padding item lebih compact
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bagian Teks
                            Column(modifier = Modifier.weight(1f)) {
                                // Keyword (Utama)
                                Text(
                                    text = rule.keyword,
                                    style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 14.sp)
                                )
                                // Detail (Kategori & Note)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Titik Warna Kategori (Visual Cue)
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CatOrange) // Bisa diganti dynamic color kalo mau
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rule.targetCategory,
                                        style = AppFont.Medium.copy(color = UIGray, fontSize = 11.sp)
                                    )
                                    // Tampilkan Note kalau ada
                                    if (!rule.targetNote.isNullOrEmpty()) {
                                        Text(
                                            text = " • ${rule.targetNote}",
                                            style = AppFont.Regular.copy(color = UIGray.copy(0.7f), fontSize = 11.sp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Tombol Hapus (Kecil & Subtle)
                            IconButton(
                                onClick = { viewModel.deleteParserRule(rule) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = UIGray.copy(0.4f), // Warna agak samar biar ga kepencet
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // --- DIVIDER (Garis Pemisah) ---
                        // Tampilkan garis KECUALI di item terakhir
                        if (index < parserRules.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp), // Indent dikit biar rapi kayak iOS
                                thickness = 0.5.dp,
                                color = UIGray.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { keyword, category, note ->
                viewModel.addParserRule(keyword, category, note)
                showAddDialog = false
            }
        )
    }
}

// Komponen Kecil untuk Judul Section
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = AppFont.Bold.copy(fontSize = 12.sp, color = UIGray, letterSpacing = 1.sp),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

// Fungsi Helper Notifikasi
@SuppressLint("MissingPermission")
fun showDummyNotification(context: Context) {
    val channelId = "flux_channel"
    val notificationId = 1
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Flux Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    val accentColor = "#0B0E14".toColorInt()

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.flux_transparent)
        .setColor(accentColor)
        .setContentTitle("Flux Budget Alert")
        .setContentText("You've used 80% of your daily budget!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(notificationId, notification)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food and Beverages") }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // List Kategori + Icon (Opsional, biar cantik)
    val categories = listOf(
        "Food and Beverages" to R.drawable.ic_food_outline,
        "Transportation" to R.drawable.ic_car_outline,
        "Groceries and Shopping" to R.drawable.ic_cart_outline, // Ganti icon sesuai resource kamu
        "Entertainment" to R.drawable.ic_ticket_outline,
        "Account Transfer" to R.drawable.ic_wallet_outline,
        "Other" to R.drawable.ic_other_outline
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D24), // Background Card (Sedikit lebih terang dari hitam pekat)
        shape = RoundedCornerShape(24.dp), // Sudut lebih bulat

        // --- TITLE ---
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(UITeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.flux_transparent), null, tint = UITeal, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "New Rule",
                    style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite)
                )
            }
        },

        // --- CONTENT INPUTS ---
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 1. INPUT KEYWORD
                FluxInput(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = "Keyword",
                    placeholder = "e.g. Kopi Kenangan",
                    icon = Icons.Default.Search // Icon pencarian
                )

                // 2. CATEGORY DROPDOWN (Fixed Version)
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Layer 1: Tampilan Visual (FluxInput)
                    FluxInput(
                        value = category,
                        onValueChange = {},
                        label = "Auto-Category",
                        placeholder = "Select Category",
                        icon = Icons.Default.List,
                        readOnly = true, // Tetap readOnly biar keyboard ga muncul
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, null, tint = UIGray)
                        }
                        // modifier clickable DIHAPUS dari sini biar ga konflik
                    )

                    // Layer 2: "Kaca Transparan" buat nangkep klik (SOLUSINYA DISINI)
                    Box(
                        modifier = Modifier
                            .matchParentSize() // Ukurannya ngikutin FluxInput di belakangnya
                            .clickable { expanded = true } // Kliknya dipindah kesini
                    )

                    // Layer 3: Dropdown Menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(Color(0xFF252932))
                            .width(260.dp)
                    ) {
                        categories.forEach { (catName, _) ->
                            DropdownMenuItem(
                                text = {
                                    Text(catName, style = AppFont.Medium.copy(color = UIWhite))
                                },
                                onClick = {
                                    category = catName
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = UIWhite,
                                    leadingIconColor = UITeal
                                )
                            )
                        }
                    }
                }

                // 3. INPUT NOTE
                FluxInput(
                    value = note,
                    onValueChange = { note = it },
                    label = "Auto-Note (Optional)",
                    placeholder = "e.g. Jajan Kopi",
                    icon = Icons.Default.Edit
                )
            }
        },

        // --- BUTTONS ---
        confirmButton = {
            Button(
                onClick = {
                    if (keyword.isNotEmpty()) {
                        onSave(keyword, category, note.ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = UITeal,
                    disabledContainerColor = UITeal.copy(0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = keyword.isNotEmpty()
            ) {
                Text(
                    "Save Rule",
                    style = AppFont.Bold.copy(color = UIBlack, fontSize = 14.sp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = AppFont.Medium.copy(color = UIGray, fontSize = 14.sp)
                )
            }
        }
    )
}

// --- KOMPONEN INPUT YANG REUSABLE (Biar kodenya rapi) ---
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
            focusedTextColor = UIWhite,
            unfocusedTextColor = UIWhite,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = UITeal,
            unfocusedBorderColor = UIGray.copy(alpha = 0.3f),
            focusedLabelColor = UITeal,
            unfocusedLabelColor = UIGray,
            cursorColor = UITeal
        ),
        textStyle = AppFont.Medium.copy(fontSize = 14.sp),
        modifier = modifier.fillMaxWidth()
    )
}