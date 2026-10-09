package com.example.flux.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.preferences.translate
import com.example.flux.ui.theme.*

@Composable
fun SearchField(value: String, change: (String) -> Unit, placeholder: String) {
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)
    TextField(value, change, modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
        .border(1.dp, if (focused) UITeal.copy(alpha = .55f) else UIBorder.copy(alpha = .35f), shape),
        placeholder = { Text(translate(placeholder), style = AppFont.Regular.copy(fontSize = 14.sp), color = UIGray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = if (focused) UITeal else UIGray, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (value.isNotEmpty()) ({ IconButton({ change("") }) { Icon(Icons.Default.Close, translate("Hapus pencarian"), tint = UIGray, modifier = Modifier.size(18.dp)) } }) else null,
        singleLine = true, textStyle = AppFont.Medium.copy(fontSize = 15.sp, color = UIWhite), shape = shape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(focusedContainerColor = UISurfaceRaised, unfocusedContainerColor = UISurfaceRaised,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = UITeal))
}

/** Equal-width, pill-shaped filters shared across the app. */
@Composable
fun FilterTabs(labels: List<String>, selected: Int, change: (Int) -> Unit) {
    Surface(color = UISurfaceRaised, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UIBorder.copy(alpha = .35f))) {
        Row(Modifier.fillMaxWidth().padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                Surface(onClick = { change(index) }, modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { role = Role.Tab; this.selected = active },
                    color = if (active) UIAccentFill else Color.Transparent, contentColor = if (active) UIOnAccent else UIGray,
                    shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.padding(horizontal = 6.dp, vertical = 11.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(translate(label), style = AppFont.SemiBold.copy(fontSize = 13.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}
