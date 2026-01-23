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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun SettingsScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var injectionAmount by remember { mutableStateOf("") }

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
                            modifier = Modifier.size(30.dp)
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
        Spacer(modifier = Modifier.height(100.dp))
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