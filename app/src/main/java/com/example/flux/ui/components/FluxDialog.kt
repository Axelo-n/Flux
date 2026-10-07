package com.example.flux.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.flux.preferences.translate
import com.example.flux.ui.theme.*

/** A compact, scrollable dialog that follows Flux's cards and input styling. */
@Composable
fun FluxDialog(title: String, subtitle: String, icon: ImageVector, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(20.dp).widthIn(max = 440.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = UISurface, border = BorderStroke(1.dp, UIBorder)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = UITeal.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                        Icon(icon, null, Modifier.padding(12.dp).size(22.dp), tint = UITeal)
                    }
                    Text(translate(title), Modifier.weight(1f), style = AppFont.Bold.copy(fontSize = 23.sp), color = UIWhite)
                    IconButton(dismiss, Modifier.size(48.dp)) { Icon(Icons.Default.Close, translate("Tutup"), tint = UIGray, modifier = Modifier.size(20.dp)) }
                }
                Hint(subtitle)
                content()
            }
        }
    }
}
