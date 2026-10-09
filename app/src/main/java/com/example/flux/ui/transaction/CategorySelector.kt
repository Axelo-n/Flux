package com.example.flux.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.preferences.translate
import com.example.flux.ui.components.FluxDialog
import com.example.flux.ui.theme.*

@Composable
fun CategorySelector(selectedCategory: String, select: (String) -> Unit, includeIncome: Boolean = false) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val visual = categoryVisual(selectedCategory)
    Surface(onClick = { focus.clearFocus(); keyboard?.hide(); expanded = true }, color = UISurface, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, UIBorder.copy(alpha = .6f)), modifier = Modifier.fillMaxWidth().semantics { contentDescription = translate("Ubah kategori") }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = visual.color.copy(alpha = .12f), shape = RoundedCornerShape(13.dp)) {
                Icon(painterResource(visual.icon), null, tint = visual.color, modifier = Modifier.padding(11.dp).size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(translate("Kategori"), color = UIGray, style = AppFont.Medium.copy(fontSize = 11.sp))
                Text(translate(selectedCategory), color = UIWhite, style = AppFont.SemiBold.copy(fontSize = 16.sp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = UIGray, modifier = Modifier.size(20.dp))
        }
    }
    if (expanded) FluxDialog("Pilih kategori", "Sesuaikan kategori transaksi ini.", Icons.Default.List, { expanded = false }) {
        val options = categoryVisuals.filter { includeIncome || it.name != "Income" }
        BoxWithConstraints {
            val columns = if (maxWidth >= 260.dp && LocalDensity.current.fontScale < 1.4f) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val emptySlots = columns - row.size
                        if (emptySlots > 0) Spacer(Modifier.weight(emptySlots / 2f))
                        row.forEach { item ->
                            val active = selectedCategory == item.name
                            Surface(onClick = { select(item.name); expanded = false }, color = if (active) UIAccentFill.copy(alpha = .12f) else UISurfaceRaised,
                                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (active) UITeal.copy(alpha = .65f) else UIBorder.copy(alpha = .2f)),
                                modifier = Modifier.weight(1f).fillMaxHeight().semantics { selected = active; role = Role.RadioButton; contentDescription = "${translate("Pilih kategori")} ${translate(item.name)}" }) {
                                Box {
                                    Column(Modifier.fillMaxSize().heightIn(min = 92.dp).padding(horizontal = 6.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                                        Icon(painterResource(item.icon), null, tint = item.color, modifier = Modifier.size(26.dp))
                                        Text(translate(item.name), color = if (active) UIWhite else UIGray, style = AppFont.Medium.copy(fontSize = 12.sp), textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (active) Icon(Icons.Default.Check, null, tint = UITeal, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(13.dp))
                                }
                            }
                        }
                        if (emptySlots > 0) Spacer(Modifier.weight(emptySlots / 2f))
                    }
                }
            }
        }
    }
}
