package com.example.flux

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

// 1. WRAPPER
@Composable
fun AnalyticsScreen(viewModel: DashboardViewModel) {
    val globalState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.analyticsDate.collectAsState()

    // Hitung data analytics on-the-fly
    val analyticsData = remember(globalState.recentTransactions, selectedDate) {
        viewModel.getMonthlyAnalytics(selectedDate, globalState.recentTransactions)
    }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dateString = monthFormat.format(selectedDate.time)

    AnalyticsScreenContent(
        analyticsData = analyticsData,
        dateString = dateString,
        onNextMonth = { viewModel.nextMonth() },
        onPrevMonth = { viewModel.prevMonth() }
    )
}

// 2. UI CONTENT
@Composable
fun AnalyticsScreenContent(
    analyticsData: MonthlyAnalyticsState,
    dateString: String,
    onNextMonth: () -> Unit,
    onPrevMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UIBlack)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // --- HEADER ---
        Text(
            text = "Analytics",
            style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite)
        )
        Text(
            text = "Where did your money go?",
            style = AppFont.Medium.copy(fontSize = 16.sp, color = UIGray)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- MONTH SELECTOR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(UIGray.copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = UIWhite)
            }

            Text(
                text = dateString,
                style = AppFont.Bold.copy(fontSize = 18.sp, color = UIWhite)
            )

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(UIGray.copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = UIWhite)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- CONTENT ---
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CHART & TOTAL
            item {
                if (analyticsData.categoryStats.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        DonutChart(
                            data = analyticsData.categoryStats,
                            modifier = Modifier.size(200.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Spent",
                                style = AppFont.SemiBold.copy(fontSize = 14.sp, color = UIGray)
                            )
                            Text(
                                text = analyticsData.totalExpense,
                                style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No expenses this month", style = AppFont.Medium.copy(color = UIGray))
                    }
                }
            }

            item {
                Text(
                    "Daily Trend",
                    style = AppFont.Bold.copy(fontSize = 18.sp, color = UIWhite),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Panggil Grafik Scrollable kita
                MonthlySpendingGraph(
                    dataPoints = analyticsData.dailyGraphData
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // INCOME SUMMARY
            item {
                FluxCard {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Income this month", style = AppFont.Medium.copy(color = UIGray))
                        Text(analyticsData.totalIncome, style = AppFont.Bold.copy(color = CatGreen))
                    }
                }
            }

            // CATEGORY LIST
            if (analyticsData.categoryStats.isNotEmpty()) {
                item {
                    Text(
                        "Breakdown",
                        style = AppFont.Bold.copy(fontSize = 18.sp, color = UIWhite),
                        modifier = Modifier
                            .padding(top = 8.dp)
                    )
                }

                items(analyticsData.categoryStats) { stat ->
                    CategoryProgressRow(stat)
                }
            }
        }
    }
}

// --- CUSTOM CHART ---
@Composable
fun DonutChart(
    data: List<CategoryStat>,
    modifier: Modifier = Modifier,
    thickness: Dp = 25.dp,
    gapAngle: Float = 5f // Jarak antar segmen (derajat)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = thickness.toPx()
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Hitung Keliling Lingkaran (2 * pi * r)
        val circumference = (2 * Math.PI * radius).toFloat()

        val strokeCapAngle = (strokeWidth / circumference) * 360f

        var currentStartAngle = -90f

        data.forEach { stat ->
            val totalAssignedAngle = stat.percentage * 360f

            val reductionAngle = gapAngle + strokeCapAngle

            val visualSweepAngle = if (data.size == 1) {
                totalAssignedAngle
            } else {
                (totalAssignedAngle - reductionAngle).coerceAtLeast(0.1f)
            }

            val startOffset = reductionAngle / 2

            drawArc(
                color = stat.color,
                startAngle = currentStartAngle + startOffset,
                sweepAngle = visualSweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                ),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            currentStartAngle += totalAssignedAngle
        }
    }
}

// --- BARIS KATEGORI ---
@Composable
fun CategoryProgressRow(stat: CategoryStat) {
    var animatedProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(stat.percentage) {
        animatedProgress = stat.percentage
    }

    val progressAnim by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000)
    )

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(stat.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // Pake icon default kalau resource ga ketemu di preview
                Icon(
                    painter = painterResource(stat.icon),
                    contentDescription = null,
                    tint = stat.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stat.category, style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                Text(
                    "${(stat.percentage * 100).toInt()}% of total",
                    style = AppFont.Medium.copy(color = UIGray, fontSize = 14.sp)
                )
            }

            Text(
                text = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    .format(stat.total).replace("Rp", "Rp "),
                style = AppFont.Bold.copy(color = UIWhite, fontSize = 18.sp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(UIBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(UIGray.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressAnim)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(stat.color)
            )
        }
    }
}

@Composable
fun MonthlySpendingGraph(
    dataPoints: List<DayData>,
    modifier: Modifier = Modifier
) {
    val maxDataValue = dataPoints.maxOfOrNull { it.amount } ?: 100000f
    val yAxisMax = maxOf(maxDataValue, 100000f) * 1.2f

    // Konfigurasi Scroll
    val scrollState = rememberScrollState()

    // Lebar satu kolom.
    val dayColumnWidth = 50.dp

    val graphWidth = dayColumnWidth * dataPoints.size

    LaunchedEffect(Unit) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    FluxCard(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize()
        ) {
            // --- FIXED Y-AXIS ---
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 24.dp)
                    .width(40.dp)
            ) {
                val step = yAxisMax / 4
                val labels = listOf(
                    yAxisMax,
                    yAxisMax - step,
                    yAxisMax - (step * 2),
                    yAxisMax - (step * 3),
                    0f
                )

                labels.forEach { value ->
                    Text(
                        text = "${(value / 1000).toInt()}k",
                        style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 10.sp),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // --- SCROLLABLE GRAPH ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                // Graph Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(graphWidth)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val paddingBottom = 10.dp.toPx()
                        val drawingHeight = size.height - paddingBottom

                        // --- GRID LINES ---
                        val stepHeight = drawingHeight / 4
                        for (i in 0..4) {
                            val yPos = (i * stepHeight)

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

                        if (dataPoints.isEmpty()) return@Canvas

                        val colWidthPx = dayColumnWidth.toPx()

                        val points = dataPoints.mapIndexed { index, dayData ->
                            val x = (index * colWidthPx) + (colWidthPx / 2f)

                            val yRatio = dayData.amount / yAxisMax
                            val y = size.height - paddingBottom - (yRatio * drawingHeight)

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
                            val dayData = dataPoints[index]
                            val isOver = dayData.amount > dayData.limit
                            val pointColor = if (isOver) UIRed else UIGreen

                            drawCircle(color = pointColor, radius = 5.dp.toPx(), center = offset)
                            drawCircle(color = UIBlack, radius = 3.dp.toPx(), center = offset)
                        }
                    }
                }

                // --- X-AXIS LABELS ---
                Row(
                    modifier = Modifier
                        .width(graphWidth)
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dataPoints.forEach {
                        Text(
                            text = it.day,
                            style = AppFont.SemiBold.copy(color = UIGray, fontSize = 12.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(dayColumnWidth)
                        )
                    }
                }
            }
        }
    }
}

// --- 3. PREVIEW SECTION ---
@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    // Dummy Data Buatan
    val dummyStats = listOf(
        CategoryStat(
            category = "Food & Beverages",
            total = 1250000.0,
            percentage = 0.5f, // 50%
            color = CatOrange,
            icon = R.drawable.ic_food_outline
        ),
        CategoryStat(
            category = "Transportation",
            total = 750000.0,
            percentage = 0.3f, // 30%
            color = CatGreen,
            icon = R.drawable.ic_car_outline
        ),
        CategoryStat(
            category = "Entertainment",
            total = 500000.0,
            percentage = 0.2f, // 20%
            color = CatYellow,
            icon = R.drawable.ic_ticket_outline
        )
    )

    val dummyState = MonthlyAnalyticsState(
        totalExpense = "Rp 2.500.000",
        totalIncome = "Rp 5.000.000",
        categoryStats = dummyStats
    )

    FluxTheme {
        AnalyticsScreenContent(
            analyticsData = dummyState,
            dateString = "January 2026",
            onNextMonth = {},
            onPrevMonth = {}
        )
    }
}