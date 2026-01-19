package com.example.flux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.AppFont
import com.example.flux.ui.theme.CatBlue
import com.example.flux.ui.theme.CatOrange
import com.example.flux.ui.theme.CatPurple
import com.example.flux.ui.theme.UIBackground
import com.example.flux.ui.theme.UIBlack
import com.example.flux.ui.theme.UIBlue
import com.example.flux.ui.theme.UIGray
import com.example.flux.ui.theme.UIGreen
import com.example.flux.ui.theme.UIRed
import com.example.flux.ui.theme.UISurface
import com.example.flux.ui.theme.UITeal
import com.example.flux.ui.theme.UITertiary
import com.example.flux.ui.theme.UIWhite

// --- DUMMY DATA MODELS ---
data class Transaction(
    val id: Int,
    val title: String,
    val category: String,
    val amount: String,
    val iconRes: Int,     // ID drawable icon
    val iconBgColor: Color, // Warna bulatannya
    val isIncome: Boolean   // Buat nentuin warna teks duit (Hijau/Merah)
)

data class DayData(
    val day: String,
    val amount: Float,
    val limit: Float
)

@Composable
fun DashboardScreen() {
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    val isListenerActive = true

    // 1. Scaffold cuma buat ngatur System Bar (Status bar atas)
    Scaffold(
        containerColor = UIBackground
        // HAPUS parameter bottomBar = { ... } dari sini!
    ) { innerPadding ->

        // 2. Gunakan Box sebagai container utama buat numpuk (Stack)
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Kita cuma butuh padding atas dari Scaffold (biar ga nabrak status bar)
                .padding(top = innerPadding.calculateTopPadding())
        ) {

            // LAYER 1: KONTEN (Paling Belakang)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                // INI KUNCINYA:
                // Kasih "bantalan" kosong di paling bawah setinggi Navbar (80dp) + Jarak (30dp) + Extra (20dp)
                // Jadi pas di-scroll mentok, item terakhir bakal naik di atas navbar.
                contentPadding = PaddingValues(bottom = 150.dp, top = 20.dp)
            ) {
                // ... (Item-item Header, Graph, dll sama kayak sebelumnya) ...

                // Item 1: Header
                item { HeaderSection(isActive = isListenerActive) }
                // Item 2: Budget Grid
                item { BudgetGridSection() }
                // Item 3: Balance
                item { BalanceRowSection() }
                // Item 4: Graph
                item { SpendingGraphSection() }
                // Item 5: Transaction List
                item { RecentTransactionsCard() }

                // FOOTER CANTIK (Pengganti Spacer kosong)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_outline), // Pake icon apa aja
                            contentDescription = null,
                            tint = UIGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You are up to date",
                            style = AppFont.Medium.copy(fontSize = 12.sp, color = UIGray.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // LAYER 2: NAVBAR (Mengambang di Depan)
            // Kita tempel di paling bawah layar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Tempel bawah
            ) {
                FluxBottomNavigation(
                    selectedIndex = selectedNavIndex,
                    onItemSelected = { selectedNavIndex = it }
                )
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun HeaderSection(isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.flux_title),
            contentDescription = "Flux Title",
            modifier = Modifier.height(20.dp),
            tint = UIWhite
        )

        // Listener Status Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(UISurface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Pulsing Animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ), label = "alpha"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isActive) UITeal.copy(alpha = alpha) else UIRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "Listening" else "Paused",
                style = AppFont.Bold.copy(
                    fontSize = 16.sp,
                    color = if (isActive) UITeal else UIGray
                )
            )
        }
    }
}

@Composable
fun BudgetGridSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Card 1: Daily Budget
        FluxCard(modifier = Modifier
            .width(200.dp)
            .height(100.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Daily Budget",
                    style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp),
                    modifier = Modifier.offset(y = (3).dp)
                )
                Text(
                    text = "Rp 60.000",
                    style = AppFont.Bold.copy(color = UIWhite, fontSize = 32.sp),
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }

        // Card 2: Weekly Usage (Updated: Battery Style)
        FluxCard(modifier = Modifier
            .height(100.dp)
            .weight(0.8f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Label Atas
                Text(
                    text = "Weekly Usage",
                    style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // THE BATTERY BAR (Canvas Custom)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp) // Ketebalan batre
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                        // 1. Track (Background Bar - Abu gelap)
                        drawRoundRect(
                            color = UIBlack.copy(alpha = 0.5f), // Lebih gelap dari card
                            cornerRadius = cornerRadius,
                            size = size
                        )

                        // 2. Progress Fill (Gradient Cyan -> Blue)
                        // Misal progress 75% -> width * 0.75f
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(UITeal, UIBlue)),
                            cornerRadius = cornerRadius,
                            size = size.copy(width = size.width * 0.75f) // Ganti 0.75f sesuai data real
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceRowSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Current Balance
        FluxCard(modifier = Modifier.weight(1f).height(80.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(text = "Current Balance", style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = (3).dp))
                Text(text = "Rp 360.000", style = AppFont.Bold.copy(color = UIWhite, fontSize = 24.sp), modifier = Modifier.offset(y = (-3).dp))
            }
        }

        // Extra Balance
        FluxCard(modifier = Modifier.weight(1f).height(80.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(text = "Extra Balance", style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = (3).dp))
                Text(
                    text = "+Rp 50.000",
                    style = AppFont.Bold.copy(color = UIGreen, fontSize = 24.sp), modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }
    }
}

@Composable
fun SpendingGraphSection() {
    val dataPoints = listOf(
        DayData("Mon", 40f, 40f),
        DayData("Tue", 40f, 40f),
        DayData("Wed", 60f, 40f),
        DayData("Thu", 30f, 40f),
        DayData("Fri", 38f, 40f),
        DayData("Sat", 80f, 60f),
        DayData("Sun", 20f, 60f)
    )

    FluxCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Gedein dikit biar lega
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(modifier = Modifier.weight(1f)) {
                // 1. Y-Axis Labels (Kiri)
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 30.dp) // Samain padding bawah biar sejajar 20k
                ) {
                    val yLabels = listOf("100k", "80k", "60k", "40k", "20k")
                    yLabels.forEach { label ->
                        Text(
                            text = label,
                            style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 16.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Graph Area (Kanan)
                Column(modifier = Modifier.weight(1f)) {

                    // CANVAS GRAPH
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Padding internal biar titik ga kepotong garis
                            val paddingBottom = 10.dp.toPx()
                            val paddingTop = 10.dp.toPx()

                            val width = size.width
                            val height = size.height

                            // Hitung area gambar efektif (biar ga nabrak atas/bawah)
                            val drawingHeight = height - paddingBottom - paddingTop

                            // Sumbu X & Y Lines
                            // Garis Vertikal (Kiri)
                            drawLine(
                                color = UIWhite,
                                start = Offset(0f, 0f),
                                end = Offset(0f, height), // Full height
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            // Garis Horizontal (Bawah)
                            drawLine(
                                color = UIWhite,
                                start = Offset(0f, height),
                                end = Offset(width, height),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Logic Koordinat (Kolom Based)
                            val maxVal = 100f
                            val minVal = 20f
                            val range = maxVal - minVal
                            val colWidth = width / dataPoints.size // Lebar per kolom

                            val points = dataPoints.mapIndexed { index, dayData ->
                                // X: Geser ke tengah kolom
                                // (index * lebarKolom) + (setengah lebarKolom)
                                val x = (index * colWidth) + (colWidth / 2f)

                                // Y: Mapping nilai dengan padding
                                val normalizedY = 1 - ((dayData.amount - minVal) / range)
                                val y = paddingTop + (normalizedY * drawingHeight)

                                Offset(x, y)
                            }

                            // Gambar Garis Penghubung
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = UIWhite,
                                    start = points[i],
                                    end = points[i + 1],
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            // Gambar Titik
                            points.forEachIndexed { index, offset ->
                                val isOver = dataPoints[index].amount > dataPoints[index].limit
                                val pointColor = if (isOver) UIRed else UIGreen

                                drawCircle(
                                    color = Color(0x00000000),
                                    radius = 8.dp.toPx(), // Border luar
                                    center = offset
                                )
                                drawCircle(
                                    color = pointColor,
                                    radius = 6.dp.toPx(), // Titik dalam
                                    center = offset
                                )
                            }
                        }
                    }

                    // X-Axis Labels (Bawah Canvas)
                    // Pake Row dengan Weight biar center-nya sama persis kayak kolom di canvas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp), // Jarak teks ke garis
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dataPoints.forEach {
                            Box(
                                modifier = Modifier.weight(1f), // Bagi rata width-nya
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = it.day,
                                    style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard() {
    // Bungkus semua dalam satu Card besar
    FluxCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Title
            Text(
                text = "Transactions Today",
                style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // List Item (Manual Column biar ga conflict scroll)
            val transactions = getDummyTransactions()

            transactions.forEachIndexed { index, transaction ->
                TransactionRowItem(transaction)

                // Kasih jarak antar item, kecuali yang terakhir
                if (index < transactions.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(transaction: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Icon Circle
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(transaction.iconBgColor), // Warna background dinamis (Ungu/Orange)
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = transaction.iconRes),
                contentDescription = null,
                tint = UIWhite, // Icon selalu putih
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Title & Category (Tengah)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = AppFont.Medium.copy(
                    color = UIWhite,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // Biar text kepanjangan jadi "..."
                modifier = Modifier.offset(y = (4).dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.category,
                style = AppFont.Medium.copy(
                    color = UIGray, // Warna abu sesuai gambar
                    fontSize = 14.sp
                ),
                modifier = Modifier.offset(y = (-4).dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Amount (Kanan)
        Text(
            text = transaction.amount,
            style = AppFont.Bold.copy(
                // Hijau kalau Income, Merah kalau Expense
                color = if (transaction.isIncome) UIGreen else UIRed,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun FluxBottomNavigation(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    val navItems = listOf(
        R.drawable.ic_home_outline,   // Index 0: Home
        R.drawable.ic_chart_outline,  // Index 1: Analytics
        R.drawable.ic_history_outline,// Index 2: Transactions
        R.drawable.ic_wallet_outline  // Index 3: Wallet
    )

    // Container buat Navigasi Mengambang
    Box(
        modifier = Modifier
            .fillMaxWidth() // Buat centering di layar
            .padding(bottom = 30.dp), // Jarak dari bawah layar biar 'ngambang'
        contentAlignment = Alignment.Center
    ) {
        // The Navbar Pill
        BoxWithConstraints(
            modifier = Modifier
                .width(315.dp) // Lebar Fix sesuai request
                .height(80.dp) // Tinggi Fix sesuai request
                .clip(RoundedCornerShape(50)) // Pill Shape bulat banget
                .background(UISurface) // Warna dasar gelap
        ) {
            val itemWidth = maxWidth / navItems.size

            // 1. Sliding Indicator (Lingkaran Cyan)
            // Kita hitung posisi X berdasarkan index yang dipilih
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicator"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset) // Ini yang bikin geser
                    .width(itemWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Lingkaran Cyan
                Box(
                    modifier = Modifier
                        .size(74.dp) // Ukuran lingkaran (pas di height 80dp)
                        .clip(CircleShape)
                        .background(UITeal) // Warna Cyan Neon
                )
            }

            // 2. Icons Row
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                navItems.forEachIndexed { index, iconRes ->
                    val isSelected = index == selectedIndex

                    // Warna icon berubah pas dilewatin lingkaran
                    // Kalau Selected -> Hitam (Biar kontras sama Cyan)
                    // Kalau Belum -> Putih (Kontras sama background gelap)
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) UIBackground else UIWhite,
                        animationSpec = tween(300),
                        label = "color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // Hapus efek ripple standar biar bersih
                            ) { onItemSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp) // Ukuran icon
                        )
                    }
                }
            }
        }
    }
}

// Helper Card
@Composable
fun FluxCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(UISurface),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

// --- DUMMY DATA ---
fun getDummyTransactions(): List<Transaction> {
    return listOf(
        Transaction(
            1, "Transfer from Michael", "Account Transfer",
            "Rp 150.000", R.drawable.ic_card_outline, CatBlue, true
        ),
        Transaction(
            2, "Warung Mba Sri", "Food and Beverages",
            "Rp 19.000", R.drawable.ic_food_outline, CatOrange, false
        ),
        Transaction(
            3, "Aeon Supermarket", "Groceries and Shopping",
            "Rp 148.300", R.drawable.ic_cart_outline, CatPurple, false
        )
    )
}

@Composable
fun BottomSpacer(){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 50.dp), // Bottom padding biar naik dr navbar
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Garis tipis banget
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(UISurface) // Warna abu gelap
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Logo atau Icon kecil pudar
        Icon(
            // Pake icon apa aja, misal icon wallet
            painter = painterResource(id = R.drawable.ic_wallet_outline),
            contentDescription = null,
            tint = UIGray.copy(alpha = 0.3f), // Pudar banget
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tulisan kecil
        Text(
            text = "You are up to date",
            style = AppFont.Medium.copy(
                fontSize = 12.sp,
                color = UIGray.copy(alpha = 0.3f) // Pudar banget
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E11) // Warna UIBackground
@Composable
fun DashboardScreenPreview() {
    DashboardScreen()
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E11)
@Composable
fun TransactionItemPreview() {
    TransactionRowItem(
        transaction = Transaction(
            id = 1,
            title = "Contoh Transaksi",
            category = "Kategori",
            amount = "- Rp 50.000",
            iconRes = R.drawable.ic_food_outline, // Pastikan resource ini ada
            iconBgColor = CatOrange,
            isIncome = true
        )
    )
}