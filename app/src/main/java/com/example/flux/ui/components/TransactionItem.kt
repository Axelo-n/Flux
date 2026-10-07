package com.example.flux.ui.components

import com.example.flux.preferences.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.model.Transaction
import com.example.flux.ui.theme.*

@Composable
fun TransactionItem(data: Transaction, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(data.iconBgColor.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(data.iconRes), null, tint = data.iconBgColor, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (data.note.isBlank()) translate(data.title) else data.title, style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (data.refund > 0) "${translate(if (data.amount == 0.0) "Cancel" else "Refund")} · ${data.refundNote}" else "${translate(data.category)} · ${translate(data.source)}", style = AppFont.Regular.copy(color = UIGray, fontSize = 12.sp), maxLines = if (data.refund > 0) 2 else 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.widthIn(max = 150.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(translate(data.formattedAmount), style = AppFont.SemiBold.copy(color = if (data.amount == 0.0) UIGray else if (data.isIncome) UIGreen else UIWhite, fontSize = 16.sp))
            Text(translate(if (data.amount == 0.0) "Dibatalkan" else if (data.isIncome) "Pemasukan" else "Pengeluaran"), style = AppFont.Regular.copy(color = UIGray, fontSize = 11.sp))
        }
    }
}
