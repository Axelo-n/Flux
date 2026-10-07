package com.example.flux.ui.home

import com.example.flux.preferences.translate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.R
import com.example.flux.model.BudgetEngine
import com.example.flux.model.BudgetTotals
import com.example.flux.model.DayData
import com.example.flux.notification.FluxNotificationListenerService
import com.example.flux.ui.components.*
import com.example.flux.ui.theme.*
import com.example.flux.viewmodel.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(viewModel: DashboardViewModel, onSettings: () -> Unit = {}, onTransaction: (Int) -> Unit = {}, onAdd: () -> Unit = {}, onHistory: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()
    val health by FluxNotificationListenerService.health.collectAsState()
    val zone = config?.timezone ?: "Asia/Jakarta"
    val today = LocalDate.now(ZoneId.of(zone))
    val rows = state.recentTransactions.filter { BudgetEngine.day(it.date, zone) == today }
    val totals = state.totals
    val luca = com.example.flux.preferences.AppPreferences.state.value.theme == "luca"
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.flux_transparent), null, tint = UIWhite, modifier = Modifier.size(24.dp))
                        Text(translate("Flux"), style = AppFont.Bold.copy(fontSize = 32.sp, letterSpacing = (-1).sp, color = UIWhite))
                    }
                    Hint(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", com.example.flux.preferences.AppPreferences.locale)))
                }
                Surface(onClick = onSettings, color = if (luca) currentPalette.text else if (health.connected) UITeal.copy(alpha = .1f) else UIWarning.copy(alpha = .1f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, if (health.connected) UITeal.copy(alpha = .2f) else UIWarning.copy(alpha = .2f))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (luca) UIAccentFill else if (health.connected) UITeal else UIWarning))
                        Text(translate(if (health.connected) "blu aktif" else "Cek blu"), color = if (luca) UIAccentFill else if (health.connected) UITeal else UIWarning, style = AppFont.SemiBold)
                    }
                }
            }
        }
        item {
            BoxWithConstraints {
                if (maxWidth >= 600.dp && LocalDensity.current.fontScale < 1.3f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BudgetHero(totals, Modifier.weight(1.25f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { BalanceTiles(totals); SpendingRoom(totals) }
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { BudgetHero(totals); BalanceTiles(totals); SpendingRoom(totals) }
            }
        }
        item { WeekOverview(state.graphData, today) }
        item {
            SectionHeading("Aktivitas hari ini") {
                TextButton(onHistory, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(translate("Riwayat"), color = UITeal)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp), tint = UITeal)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Hint("${rows.size} transaksi tercatat")
                FilledTonalButton(onAdd, colors = ButtonDefaults.filledTonalButtonColors(containerColor = UITeal.copy(alpha = .12f), contentColor = UITeal), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(translate("Catat manual"))
                }
            }
        }
        if (rows.isEmpty()) item {
            Surface(color = UISurface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, UIBorder)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconBadge(R.drawable.ic_history_outline, UITeal)
                    Column(Modifier.weight(1f)) {
                        Text(translate("Hari baru, catatan baru"), style = AppFont.SemiBold, color = UIWhite)
                        Hint("Transaksi blu akan muncul di sini. Bisa catat manual juga.")
                    }
                }
            }
        }
        items(rows, key = { it.id }) { tx -> Surface(color = UISurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .5f))) { TransactionItem(tx) { onTransaction(tx.id) } } }
    }
}

@Composable
private fun BudgetHero(totals: BudgetTotals, modifier: Modifier = Modifier) {
    val ratio = if (totals.budget > 0) (totals.spent.toFloat() / totals.budget).coerceIn(0f, 1f) else if (totals.spent > 0) 1f else 0f
    val percent = if (totals.budget > 0) (totals.spent.toDouble() / totals.budget * 100).toLong() else 0L
    val dark = currentPalette.heroText
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(gradientBrush).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(translate("SISA JATAH HARI INI"), style = AppFont.SemiBold.copy(fontSize = 12.sp, letterSpacing = 1.sp, color = dark.copy(alpha = .8f)))
        BoxWithConstraints {
            val showRing = maxWidth >= 260.dp && LocalDensity.current.fontScale < 1.3f
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    MoneyValue(totals.remaining.coerceAtLeast(0), dark, 38.sp)
                    Text(translate(if (totals.remaining < 0) "${rupiah(-totals.remaining)} memakai extra" else "Dari jatah ${rupiah(totals.budget)}"), style = AppFont.Medium.copy(color = dark.copy(alpha = .8f), fontSize = 14.sp))
                }
                if (showRing) Box(Modifier.size(70.dp).semantics { contentDescription = "$percent persen budget terpakai" }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                        drawArc(dark.copy(alpha = .12f), -90f, 360f, false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                        if (ratio > 0f) drawArc(dark, -90f, ratio * 360, false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text(translate(if (totals.budget == 0L && totals.spent > 0) "Habis" else if (percent > 999) ">999%" else "$percent%"), style = AppFont.Bold.copy(fontSize = 18.sp, color = dark))
                }
            }
        }
        HorizontalDivider(color = dark.copy(alpha = .15f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(translate("Terpakai hari ini"), style = AppFont.Medium.copy(color = dark, fontSize = 14.sp), modifier = Modifier.weight(1f))
            Box(Modifier.weight(1f)) { MoneyValue(totals.spent, dark, 20.sp, TextAlign.End) }
        }
    }
}

@Composable
private fun BalanceTiles(totals: BudgetTotals) {
    @Composable fun Extra(modifier: Modifier) { MetricTile("Extra tersedia", totals.extra, R.drawable.ic_other_outline, if (totals.extra >= 0) UITeal else UIRed, if (totals.extra >= 0) "Sisa hari sebelumnya" else "Melampaui jatah & extra", modifier) }
    @Composable fun Balance(modifier: Modifier) { MetricTile("Saldo blu", totals.balance, R.drawable.ic_wallet_outline, UIBlue, "Berdasarkan catatan", modifier, emphasized = true) }
    BoxWithConstraints {
        if (maxWidth >= 300.dp && LocalDensity.current.fontScale < 1.3f) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Extra(Modifier.weight(1f)); Balance(Modifier.weight(1f)) }
        else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Extra(Modifier.fillMaxWidth()); Balance(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun MetricTile(title: String, amount: Long, icon: Int, accent: Color, subtitle: String, modifier: Modifier, emphasized: Boolean = false) {
    val colors = cardColors(emphasized)
    val inverted = emphasized && com.example.flux.preferences.AppPreferences.state.value.theme == "luca"
    Surface(modifier, color = colors.surface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, colors.border.copy(alpha = .7f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(icon), null, Modifier.size(18.dp), tint = if (inverted) colors.accent else accent)
                Text(translate(title), style = AppFont.Medium.copy(color = colors.muted, fontSize = 14.sp))
            }
            MoneyValue(amount, if (amount < 0) colors.negative else colors.text, 26.sp)
            Text(translate(subtitle), style = AppFont.Regular.copy(color = colors.muted, fontSize = 12.sp))
        }
    }
}

@Composable
private fun SpendingRoom(totals: BudgetTotals) {
    val luca = com.example.flux.preferences.AppPreferences.state.value.theme == "luca"
    Surface(color = if (luca) UIAccentFill.copy(alpha = .22f) else UITeal.copy(alpha = .06f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UITeal.copy(alpha = .16f))) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(translate("Ruang belanja"), style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 16.sp))
                    Text(translate("Jatah + extra, dibatasi saldo"), style = AppFont.Regular.copy(color = UIGray, fontSize = 12.sp))
                }
                Box(Modifier.weight(1f)) { MoneyValue(totals.available.coerceAtLeast(0), UITeal, 24.sp, TextAlign.End) }
            }
            if (totals.extra < 0) Hint("Extra minus akan mengurangi sisa yang terkumpul berikutnya.")
            if (totals.extra > totals.balance.coerceAtLeast(0)) Hint("Extra melebihi saldo. Periksa dana dan catatan transaksi.")
        }
    }
}

@Composable
private fun WeekOverview(data: List<DayData>, today: LocalDate) {
    val peak = maxOf(data.maxOfOrNull { maxOf(it.amount, it.limit) } ?: 0f, 1f)
    Surface(color = UISurface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .7f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(translate("Ritme belanja"), style = AppFont.SemiBold.copy(fontSize = 19.sp, color = UIWhite))
                    Text(translate("Tujuh hari terakhir"), style = AppFont.Regular.copy(color = UIGray, fontSize = 12.sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(UITeal))
                    Text(translate("Pengeluaran"), style = AppFont.Regular.copy(fontSize = 12.sp, color = UIGray))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                data.forEachIndexed { index, point ->
                    val date = today.minusDays((data.lastIndex - index).toLong())
                    val isToday = index == data.lastIndex
                    val accent = if (point.amount > point.limit) UIRed else if (isToday) UITeal else UITeal.copy(alpha = .55f)
                    Column(Modifier.weight(1f).semantics { contentDescription = "${date}: pengeluaran ${rupiah(point.amount.toLong())}, budget ${rupiah(point.limit.toLong())}" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(6.dp)).background(UISurfaceRaised), contentAlignment = Alignment.BottomCenter) {
                            if (point.amount > 0) Box(Modifier.fillMaxWidth().fillMaxHeight((point.amount / peak).coerceIn(.04f, 1f)).clip(RoundedCornerShape(6.dp)).background(accent))
                            Canvas(Modifier.fillMaxSize()) {
                                val y = (size.height * (1 - point.limit / peak)).coerceIn(1.dp.toPx(), size.height - 1.dp.toPx())
                                drawLine(UIGray.copy(alpha = .5f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                            }
                        }
                        Text(translate(if (isToday) "Kini" else date.format(DateTimeFormatter.ofPattern("EE", com.example.flux.preferences.AppPreferences.locale))), style = AppFont.Medium.copy(fontSize = 11.sp, color = if (isToday) UITeal else UIGray))
                    }
                }
            }
            Text(translate("Garis kecil = jatah harian · Sisa hari ini masuk extra besok"), style = AppFont.Regular.copy(fontSize = 12.sp, color = UIGray))
        }
    }
}

@Composable
private fun IconBadge(icon: Int, color: Color) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = .1f)), contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, Modifier.size(20.dp), tint = color) }
}
