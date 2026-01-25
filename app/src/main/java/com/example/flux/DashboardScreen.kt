package com.example.flux

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flux.ui.theme.AppFont
import com.example.flux.ui.theme.CatBlue
import com.example.flux.ui.theme.UIBackground
import com.example.flux.ui.theme.UIBlack
import com.example.flux.ui.theme.UIBlue
import com.example.flux.ui.theme.UIGray
import com.example.flux.ui.theme.UIGreen
import com.example.flux.ui.theme.UIRed
import com.example.flux.ui.theme.UISurface
import com.example.flux.ui.theme.UITeal
import com.example.flux.ui.theme.UIWhite
import java.util.Calendar

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: FluxRoutes.HOME

    // Logic: Tampilkan Navbar & FAB HANYA di halaman utama (Home, Wallet, dll)
    // Kalau lagi di halaman "Add Transaction", sembunyikan.
    val showBottomComponents = currentRoute in listOf(
        FluxRoutes.HOME, FluxRoutes.ANALYTICS, FluxRoutes.HISTORY, FluxRoutes.WALLET
    )

    val selectedNavIndex = when (currentRoute) {
        FluxRoutes.HOME -> 0
        FluxRoutes.ANALYTICS -> 1
        FluxRoutes.HISTORY -> 2
        FluxRoutes.WALLET -> 3
        else -> 0
    }

    Scaffold(
        containerColor = UIBackground,
        // FAB (Tombol Tambah)
        floatingActionButton = {
            // Animasi Show/Hide FAB
            androidx.compose.animation.AnimatedVisibility(
                visible = showBottomComponents,
                enter = androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(FluxRoutes.ADD_TRANSACTION) },
                    containerColor = UITeal,
                    contentColor = UIBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 100.dp)
                        .size(56.dp)
                        .shadow(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // LAYER 1: NAV HOST
            NavHost(
                navController = navController,
                startDestination = FluxRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(FluxRoutes.HOME) { HomeScreen(viewModel = viewModel) }
                composable(FluxRoutes.ANALYTICS) { AnalyticsScreen(viewModel = viewModel) }
                composable(FluxRoutes.HISTORY) {
                    HistoryScreen(
                        viewModel = viewModel,
                        navController = navController,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(FluxRoutes.WALLET) {
                    SettingsScreen(viewModel = viewModel)
                }

                // ADD TRANSACTION
                composable(FluxRoutes.ADD_TRANSACTION) {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("edit_transaction/{txId}") { backStackEntry ->
                    val txId = backStackEntry.arguments?.getString("txId")?.toIntOrNull()

                    if (txId != null) {
                        EditTransactionScreen(
                            viewModel = viewModel,
                            transactionId = txId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            // LAYER 2: NAVBAR
            if (showBottomComponents) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    FluxBottomNavigation(
                        selectedIndex = selectedNavIndex,
                        onItemSelected = { index ->
                            val route = when(index) {
                                0 -> FluxRoutes.HOME
                                1 -> FluxRoutes.ANALYTICS
                                2 -> FluxRoutes.HISTORY
                                3 -> FluxRoutes.WALLET
                                else -> FluxRoutes.HOME
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
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
fun BudgetGridSection(dailyLeft: String, dailyUsagePercent: Float) {
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
                    text = dailyLeft,
                    style = AppFont.Bold.copy(color = UIWhite, fontSize = 32.sp),
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }

        // Card 2: Daily Usage
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
                    text = "Daily Usage",
                    style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // THE BATTERY BAR (Canvas Custom)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                        // 1. Track (Background Bar)
                        drawRoundRect(
                            color = UIBlack.copy(alpha = 0.5f),
                            cornerRadius = cornerRadius,
                            size = size
                        )

                        // 2. Progress Fill
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(UITeal, UIBlue)),
                            cornerRadius = cornerRadius,
                            size = size.copy(width = size.width * dailyUsagePercent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceRowSection(currentBalance: String, extraBalance: String, isPositive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Current Balance
        FluxCard(modifier = Modifier.weight(1f).height(80.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(text = "Current Balance", style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = (3).dp))
                Text(text = currentBalance, style = AppFont.Bold.copy(color = UIWhite, fontSize = 20.sp), modifier = Modifier.offset(y = (-3).dp))
            }
        }

        // Extra Balance
        FluxCard(modifier = Modifier.weight(1f).height(80.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(text = "Extra Balance", style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = (3).dp))
                Text(
                    text = extraBalance,
                    style = AppFont.Bold.copy(
                        color = if (isPositive) UIGreen else UIRed,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }
    }
}

@Composable
fun SpendingGraphSection(dataPoints: List<DayData>) {
    val maxDataValue = dataPoints.maxOfOrNull { it.amount } ?: 80000f
    val yAxisMax = maxOf(maxDataValue, 80000f) * 1.2f

    FluxCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(modifier = Modifier.weight(1f)) {
                // 1. Y-Axis Labels (Dinamis)
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 30.dp)
                ) {
                    // Generate 5 label dari 0 -> Max
                    val step = yAxisMax / 4
                    val labels = listOf(
                        yAxisMax,
                        yAxisMax - step,
                        yAxisMax - (step * 2),
                        yAxisMax - (step * 3),
                        0f // Paling bawah 0
                    )

                    labels.forEach { value ->
                        Text(
                            text = "${(value / 1000).toInt()}k", // Format 100k
                            style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 12.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Graph Area
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val paddingBottom = 10.dp.toPx()
                            val paddingTop = 10.dp.toPx()
                            val drawingHeight = size.height - paddingBottom - paddingTop

                            // --- GRID LINES ---
                            val stepHeight = drawingHeight / 4
                            for (i in 0..4) {
                                val yPos = (i * stepHeight) + paddingTop

                                drawLine(
                                    color = UIGray.copy(alpha = 0.3f),
                                    start = Offset(0f, yPos),
                                    end = Offset(size.width, yPos),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(10f, 10f),
                                        0f
                                    )
                                )
                            }

                            // Garis Axis
                            drawLine(
                                color = UIWhite.copy(alpha = 0.5f),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = UIWhite.copy(alpha = 0.5f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 2.dp.toPx()
                            )

                            if (dataPoints.isEmpty()) return@Canvas

                            val colWidth = size.width / dataPoints.size

                            // Map Data ke Koordinat
                            val points = dataPoints.mapIndexed { index, dayData ->
                                val x = (index * colWidth) + (colWidth / 2f)

                                // Rumus Y Dinamis: (Value / Max) * Height
                                val yRatio = dayData.amount / yAxisMax
                                // Invert Y (Karena canvas 0 nya di atas)
                                val y = size.height - paddingBottom - (yRatio * drawingHeight)

                                Offset(x, y)
                            }

                            // Gambar Garis
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
                                val dayData = dataPoints[index]
                                val isOver = dayData.amount > dayData.limit
                                val pointColor = if (isOver) UIRed else UIGreen

                                drawCircle(color = pointColor, radius = 6.dp.toPx(), center = offset) // Border
                                drawCircle(color = pointColor, radius = 4.dp.toPx(), center = offset) // Isi
                            }
                        }
                    }

                    // X-Axis Labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dataPoints.forEach {
                            Text(
                                text = it.day,
                                style = AppFont.SemiBold.copy(color = UIGray, fontSize = 12.sp),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard(transactions: List<Transaction>) {
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

            val calendar = Calendar.getInstance()
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
            val todayYear = calendar.get(Calendar.YEAR)

            val todaysTransactions = transactions.filter { transaction ->
                val txCalendar = Calendar.getInstance()
                txCalendar.timeInMillis = transaction.date

                // Bandingkan Hari dan Tahun
                txCalendar.get(Calendar.DAY_OF_YEAR) == todayDay &&
                        txCalendar.get(Calendar.YEAR) == todayYear
            }
            if (todaysTransactions.isEmpty()) {
                Text(
                    text = "No transactions today",
                    style = AppFont.Regular.copy(color = UIGray, fontSize = 14.sp),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                todaysTransactions.forEachIndexed { index, transaction ->
                    TransactionRowItem(transaction)
                    if (index < todaysTransactions.size - 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
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
                .background(transaction.iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = transaction.iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(y = (4).dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.category,
                style = AppFont.Medium.copy(
                    color = UIGray,
                    fontSize = 14.sp
                ),
                modifier = Modifier.offset(y = (-4).dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Amount (Kanan)
        Text(
            text = transaction.formattedAmount,
            style = AppFont.Bold.copy(
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

    // Container untuk Navigasi Mengambang
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        // The Navbar Pill
        BoxWithConstraints(
            modifier = Modifier
                .width(315.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(50))
                .background(UISurface)
        ) {
            val itemWidth = maxWidth / navItems.size

            // 1. Sliding Indicator
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
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Lingkaran Cyan
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(UITeal)
                )
            }

            // 2. Icons Row
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                navItems.forEachIndexed { index, iconRes ->
                    val isSelected = index == selectedIndex
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
                                indication = null
                            ) { onItemSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
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

@Composable
fun BottomSpacer(){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Garis tipis banget
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(UISurface)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Logo atau Icon kecil pudar
        Icon(
            painter = painterResource(id = R.drawable.ic_wallet_outline),
            contentDescription = null,
            tint = UIGray.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tulisan kecil
        Text(
            text = "You are up to date",
            style = AppFont.Medium.copy(
                fontSize = 12.sp,
                color = UIGray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isListenerActive by remember { mutableStateOf(false) }

    // Fungsi Cek Izin Notifikasi
    fun checkStatus() {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isListenerActive = flat != null && flat.contains(context.packageName)
    }

    // Cek setiap kali layar tampil (Resume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkStatus()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 150.dp, top = 20.dp)
    ) {
        item { HeaderSection(isActive = isListenerActive) }

        item {
            BudgetGridSection(
                dailyLeft = state.dailyBudgetLeft,
                dailyUsagePercent = state.dailyUsagePercent
            )
        }

        item {
            BalanceRowSection(
                currentBalance = state.currentBalance,
                extraBalance = state.extraBalance,
                isPositive = state.isExtraBalancePositive
            )
        }

        item { SpendingGraphSection(dataPoints = state.graphData) }

        item { RecentTransactionsCard(transactions = state.recentTransactions) }

        item { BottomSpacer() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E11)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(viewModel())
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E11)
@Composable
fun TransactionItemPreview() {
    TransactionRowItem(
        Transaction(
            id = 1,
            title = "Transfer from Michael",
            category = "Account Transfer",
            amount = 150000.0,
            formattedAmount = "Rp 150.000",
            iconRes = R.drawable.ic_card_outline,
            iconBgColor = CatBlue,
            isIncome = true,
            date = System.currentTimeMillis() - 86400000
        )
    )
}