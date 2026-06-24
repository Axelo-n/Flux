package com.example.flux.ui.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.flux.model.CategoryStat
import com.example.flux.model.DayData
import com.example.flux.model.MonthlyAnalyticsState
import com.example.flux.ui.components.FluxCard
import com.example.flux.ui.theme.AppFont
import com.example.flux.ui.theme.CatGreen
import com.example.flux.ui.theme.CatOrange
import com.example.flux.ui.theme.CatYellow
import com.example.flux.ui.theme.FluxTheme
import com.example.flux.ui.theme.UIBlack
import com.example.flux.ui.theme.UIGray
import com.example.flux.ui.theme.UIGreen
import com.example.flux.ui.theme.UIRed
import com.example.flux.ui.theme.UIWhite
import com.example.flux.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.flux.R

@Composable
fun AnalyticsScreen(viewModel: DashboardViewModel) {
    val globalState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.analyticsDate.collectAsState()

    val analyticsData = remember(globalState.recentTransactions, selectedDate) {
        viewModel.getMonthlyAnalytics(selectedDate, globalState.recentTransactions)
    }
    val dateString = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate.time)

    AnalyticsScreenContent(
        analyticsData = analyticsData,
        dateString = dateString,
        onNextMonth = { viewModel.nextMonth() },
        onPrevMonth = { viewModel.prevMonth() }
    )
}

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

        Text("Analytics", style = AppFont.Bold.copy(fontSize = 32.sp, color = UIWhite))
        Text("Where did your money go?", style = AppFont.Medium.copy(fontSize = 16.sp, color = UIGray))

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(UIGray.copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = UIWhite)
            }
            Text(dateString, style = AppFont.Bold.copy(fontSize = 18.sp, color = UIWhite))
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(UIGray.copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = UIWhite)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (analyticsData.categoryStats.isNotEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(250.dp)) {
                        DonutChart(data = analyticsData.categoryStats, modifier = Modifier.size(200.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Spent", style = AppFont.SemiBold.copy(fontSize = 14.sp, color = UIGray))
                            Text(analyticsData.totalExpense, style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite))
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses this month", style = AppFont.Medium.copy(color = UIGray))
                    }
                }
            }

            item {
                Text("Daily Trend", style = AppFont.Bold.copy(fontSize = 18.sp, color = UIWhite), modifier = Modifier.padding(bottom = 12.dp))
                MonthlySpendingGraph(dataPoints = analyticsData.dailyGraphData)
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                FluxCard {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Income this month", style = AppFont.Medium.copy(color = UIGray))
                        Text(analyticsData.totalIncome, style = AppFont.Bold.copy(color = CatGreen))
                    }
                }
            }

            if (analyticsData.categoryStats.isNotEmpty()) {
                item {
                    Text("Breakdown", style = AppFont.Bold.copy(fontSize = 18.sp, color = UIWhite), modifier = Modifier.padding(top = 8.dp))
                }
                items(analyticsData.categoryStats) { stat ->
                    CategoryProgressRow(stat)
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun DonutChart(
    data: List<CategoryStat>,
    modifier: Modifier = Modifier,
    thickness: Dp = 25.dp,
    gapAngle: Float = 5f
) {
    Canvas(modifier = modifier) {
        val strokeWidth = thickness.toPx()
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)
        val circumference = (2 * Math.PI * radius).toFloat()
        val strokeCapAngle = (strokeWidth / circumference) * 360f
        var currentStartAngle = -90f

        data.forEach { stat ->
            val totalAssignedAngle = stat.percentage * 360f
            val reductionAngle = gapAngle + strokeCapAngle
            val visualSweepAngle = if (data.size == 1) totalAssignedAngle else (totalAssignedAngle - reductionAngle).coerceAtLeast(0.1f)

            drawArc(
                color = stat.color,
                startAngle = currentStartAngle + reductionAngle / 2,
                sweepAngle = visualSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            currentStartAngle += totalAssignedAngle
        }
    }
}

@Composable
private fun CategoryProgressRow(stat: CategoryStat) {
    var animatedProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(stat.percentage) { animatedProgress = stat.percentage }
    val progressAnim by animateFloatAsState(targetValue = animatedProgress, animationSpec = tween(1000), label = "progress")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(stat.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(stat.icon), contentDescription = null, tint = stat.color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stat.category, style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
                Text("${(stat.percentage * 100).toInt()}% of total", style = AppFont.Medium.copy(color = UIGray, fontSize = 14.sp))
            }
            Text(
                text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(stat.total).replace("Rp", "Rp "),
                style = AppFont.Bold.copy(color = UIWhite, fontSize = 18.sp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(10.dp)).background(UIBlack)) {
            Box(modifier = Modifier.fillMaxSize().background(UIGray.copy(alpha = 0.1f)))
            Box(modifier = Modifier.fillMaxWidth(progressAnim).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(stat.color))
        }
    }
}

@Composable
private fun MonthlySpendingGraph(dataPoints: List<DayData>, modifier: Modifier = Modifier) {
    val maxDataValue = dataPoints.maxOfOrNull { it.amount } ?: 100000f
    val yAxisMax = maxOf(maxDataValue, 100000f) * 1.2f
    val scrollState = rememberScrollState()
    val dayColumnWidth = 50.dp
    val graphWidth = dayColumnWidth * dataPoints.size

    LaunchedEffect(Unit) { scrollState.animateScrollTo(scrollState.maxValue) }

    FluxCard(modifier = modifier.fillMaxWidth().height(250.dp)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxHeight().padding(bottom = 24.dp).width(40.dp)
            ) {
                val step = yAxisMax / 4
                listOf(yAxisMax, yAxisMax - step, yAxisMax - step * 2, yAxisMax - step * 3, 0f).forEach { value ->
                    Text("${(value / 1000).toInt()}k", style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 10.sp), textAlign = TextAlign.End)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                Box(modifier = Modifier.weight(1f).width(graphWidth)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val paddingBottom = 10.dp.toPx()
                        val drawingHeight = size.height - paddingBottom
                        val stepHeight = drawingHeight / 4

                        for (i in 0..4) {
                            drawLine(
                                color = UIGray.copy(alpha = 0.3f),
                                start = Offset(0f, i * stepHeight),
                                end = Offset(size.width, i * stepHeight),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        if (dataPoints.isEmpty()) return@Canvas

                        val colWidthPx = dayColumnWidth.toPx()
                        val points = dataPoints.mapIndexed { index, dayData ->
                            Offset(
                                x = index * colWidthPx + colWidthPx / 2f,
                                y = size.height - paddingBottom - (dayData.amount / yAxisMax) * drawingHeight
                            )
                        }

                        for (i in 0 until points.size - 1) {
                            drawLine(color = UIWhite, start = points[i], end = points[i + 1], strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                        }

                        points.forEachIndexed { index, offset ->
                            val pointColor = if (dataPoints[index].amount > dataPoints[index].limit) UIRed else UIGreen
                            drawCircle(color = pointColor, radius = 5.dp.toPx(), center = offset)
                            drawCircle(color = UIBlack, radius = 3.dp.toPx(), center = offset)
                        }
                    }
                }

                Row(modifier = Modifier.width(graphWidth).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    dataPoints.forEach {
                        Text(text = it.day, style = AppFont.SemiBold.copy(color = UIGray, fontSize = 12.sp), textAlign = TextAlign.Center, modifier = Modifier.width(dayColumnWidth))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsScreenPreview() {
    val dummyStats = listOf(
        CategoryStat("Food & Beverages", 1250000.0, 0.5f, CatOrange, R.drawable.ic_food_outline),
        CategoryStat("Transportation", 750000.0, 0.3f, CatGreen, R.drawable.ic_car_outline),
        CategoryStat("Entertainment", 500000.0, 0.2f, CatYellow, R.drawable.ic_ticket_outline)
    )
    FluxTheme {
        AnalyticsScreenContent(
            analyticsData = MonthlyAnalyticsState(totalExpense = "Rp 2.500.000", totalIncome = "Rp 5.000.000", categoryStats = dummyStats),
            dateString = "January 2026",
            onNextMonth = {},
            onPrevMonth = {}
        )
    }
}
