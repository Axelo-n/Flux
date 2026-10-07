package com.example.flux.ui.home

import com.example.flux.preferences.translate

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.R
import com.example.flux.model.DayData
import com.example.flux.model.Transaction
import com.example.flux.ui.components.FluxCard
import com.example.flux.ui.components.TransactionItem
import com.example.flux.ui.theme.AppFont
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
fun HeaderSection(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(UISurface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isActive) UITeal.copy(alpha = alpha) else UIRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = translate(if (isActive) "Listening" else "Paused"),
                style = AppFont.Bold.copy(fontSize = 16.sp, color = if (isActive) UITeal else UIGray)
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
        FluxCard(modifier = Modifier.width(200.dp).height(100.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(translate("Daily Budget"), style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = 3.dp))
                Text(translate(dailyLeft), style = AppFont.Bold.copy(color = UIWhite, fontSize = 32.sp), modifier = Modifier.offset(y = (-3).dp))
            }
        }

        FluxCard(modifier = Modifier.height(100.dp).weight(0.8f)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(translate("Daily Usage"), style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        drawRoundRect(color = UIBlack.copy(alpha = 0.5f), cornerRadius = cornerRadius, size = size)
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
        FluxCard(modifier = Modifier.weight(1f).height(80.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(translate("Current Balance"), style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = 3.dp))
                Text(translate(currentBalance), style = AppFont.Bold.copy(color = UIWhite, fontSize = 20.sp), modifier = Modifier.offset(y = (-3).dp))
            }
        }
        FluxCard(modifier = Modifier.weight(1f).height(80.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(translate("Extra Balance"), style = AppFont.SemiBold.copy(color = UIGray, fontSize = 16.sp), modifier = Modifier.offset(y = 3.dp))
                Text(translate(extraBalance), style = AppFont.Bold.copy(color = if (isPositive) UIGreen else UIRed, fontSize = 20.sp), modifier = Modifier.offset(y = (-3).dp))
            }
        }
    }
}

@Composable
fun SpendingGraphSection(dataPoints: List<DayData>) {
    val maxDataValue = dataPoints.maxOfOrNull { it.amount } ?: 80000f
    val yAxisMax = maxOf(maxDataValue, 80000f) * 1.2f

    FluxCard(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.weight(1f)) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxHeight().padding(bottom = 30.dp)
                ) {
                    val step = yAxisMax / 4
                    listOf(yAxisMax, yAxisMax - step, yAxisMax - step * 2, yAxisMax - step * 3, 0f).forEach { value ->
                        Text(translate("${(value / 1000).toInt()}k"), style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 12.sp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val paddingBottom = 10.dp.toPx()
                            val paddingTop = 10.dp.toPx()
                            val drawingHeight = size.height - paddingBottom - paddingTop
                            val stepHeight = drawingHeight / 4

                            for (i in 0..4) {
                                drawLine(
                                    color = UIGray.copy(alpha = 0.3f),
                                    start = Offset(0f, i * stepHeight + paddingTop),
                                    end = Offset(size.width, i * stepHeight + paddingTop),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            drawLine(color = UIWhite.copy(alpha = 0.5f), start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
                            drawLine(color = UIWhite.copy(alpha = 0.5f), start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 2.dp.toPx())

                            if (dataPoints.isEmpty()) return@Canvas

                            val colWidth = size.width / dataPoints.size
                            val points = dataPoints.mapIndexed { index, dayData ->
                                val x = index * colWidth + colWidth / 2f
                                val y = size.height - paddingBottom - (dayData.amount / yAxisMax) * drawingHeight
                                Offset(x, y)
                            }

                            for (i in 0 until points.size - 1) {
                                drawLine(color = UIWhite, start = points[i], end = points[i + 1], strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                            }

                            points.forEachIndexed { index, offset ->
                                val pointColor = if (dataPoints[index].amount > dataPoints[index].limit) UIRed else UIGreen
                                drawCircle(color = pointColor, radius = 6.dp.toPx(), center = offset)
                                drawCircle(color = pointColor, radius = 4.dp.toPx(), center = offset)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        dataPoints.forEach {
                            Text(text = translate(it.day), style = AppFont.SemiBold.copy(color = UIGray, fontSize = 12.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard(transactions: List<Transaction>) {
    val calendar = Calendar.getInstance()
    val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
    val todayYear = calendar.get(Calendar.YEAR)

    val todaysTransactions = transactions.filter { tx ->
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
        txCal.get(Calendar.DAY_OF_YEAR) == todayDay && txCal.get(Calendar.YEAR) == todayYear
    }

    FluxCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(translate("Transactions Today"), style = AppFont.Bold.copy(color = UIWhite, fontSize = 16.sp))
            Spacer(modifier = Modifier.height(10.dp))

            if (todaysTransactions.isEmpty()) {
                Text(translate("No transactions today"), style = AppFont.Regular.copy(color = UIGray, fontSize = 14.sp), modifier = Modifier.padding(vertical = 20.dp))
            } else {
                todaysTransactions.forEachIndexed { index, transaction ->
                    TransactionItem(data = transaction)
                    if (index < todaysTransactions.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun BottomSpacer() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(UISurface)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            painter = painterResource(R.drawable.ic_wallet_outline),
            contentDescription = null,
            tint = UIGray.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(translate("You are up to date"), style = AppFont.Medium.copy(fontSize = 12.sp, color = UIGray.copy(alpha = 0.3f)))
    }
}
