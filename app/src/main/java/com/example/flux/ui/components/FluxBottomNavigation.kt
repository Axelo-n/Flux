package com.example.flux.ui.components

import com.example.flux.preferences.translate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.R
import com.example.flux.ui.theme.*

@Composable
fun FluxBottomNavigation(selectedIndex: Int, onItemSelected: (Int) -> Unit, onAdd: () -> Unit = {}) {
    val icons = listOf(R.drawable.ic_home_outline, R.drawable.ic_chart_outline, R.drawable.ic_history_outline, R.drawable.ic_wallet_outline)
    val labels = listOf("Hari ini", "Analisis", "Riwayat", "Sistem")
    Surface(shape = RoundedCornerShape(26.dp), color = UISurface, border = BorderStroke(1.dp, UIBorder), shadowElevation = 12.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            (0..4).forEach { slot ->
                if (slot == 2) Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(onClick = onAdd, color = UIAccentFill, contentColor = UIOnAccent, shape = CircleShape, modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, translate("Catat transaksi"), Modifier.size(24.dp)) } }
                    Text(translate("Catat"), style = AppFont.Medium.copy(fontSize = 11.sp, color = UITeal))
                } else {
                    val index = if (slot < 2) slot else slot - 1
                    val selected = index == selectedIndex
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).selectable(selected, role = Role.Tab, onClick = { onItemSelected(index) }).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (index == 3) Icon(Icons.Default.Settings, null, Modifier.size(21.dp), tint = if (selected) UITeal else UIGray)
                        else Icon(painterResource(icons[index]), null, Modifier.size(21.dp), tint = if (selected) UITeal else UIGray)
                        Text(translate(labels[index]), style = (if (selected) AppFont.Bold else AppFont.Medium).copy(fontSize = 11.sp, color = if (selected) UITeal else UIGray))
                    }
                }
            }
        }
    }
}
