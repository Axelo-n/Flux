package com.example.flux.ui.components

import com.example.flux.preferences.translate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.*

/** Filled inputs used by onboarding, budget scheduling and transaction forms. */
@Composable
fun FormField(value: String, change: (String) -> Unit, label: String, modifier: Modifier = Modifier, money: Boolean = false, prominent: Boolean = false, singleLine: Boolean = true, isError: Boolean = false, placeholder: String = "", trailingIcon: @Composable (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    TextField(value, change,
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(1.dp, if (isError) UIRed else if (focused) UITeal.copy(alpha = .6f) else UIBorder.copy(alpha = .35f), shape),
        label = { Text(translate(label), style = AppFont.Medium) },
        placeholder = { Text(translate(placeholder), style = AppFont.Regular.copy(fontSize = if (prominent) 32.sp else 17.sp), color = UIGray) },
        prefix = if (money) ({ Text(translate("Rp "), style = AppFont.SemiBold.copy(fontSize = if (prominent) 24.sp else 16.sp), color = UIGray) }) else null,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = AppFont.SemiBold.copy(fontSize = if (prominent) 32.sp else 17.sp, color = UIWhite),
        keyboardOptions = KeyboardOptions(keyboardType = if (money) KeyboardType.Number else KeyboardType.Text, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        visualTransformation = if (money) RupiahGrouping else VisualTransformation.None,
        shape = shape, isError = isError,
        colors = TextFieldDefaults.colors(focusedContainerColor = UISurfaceRaised, unfocusedContainerColor = UISurfaceRaised, errorContainerColor = UISurfaceRaised, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedLabelColor = UITeal, unfocusedLabelColor = UIGray, cursorColor = UITeal)
    )
}

private object RupiahGrouping : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val grouped = buildString { original.forEachIndexed { i, char -> if (i > 0 && (original.length - i) % 3 == 0) append('.'); append(char) } }
        val offsets = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                var digits = 0
                grouped.forEachIndexed { i, c -> if (c != '.') digits++; if (digits == offset && c != '.') return i + 1 }
                return grouped.length
            }
            override fun transformedToOriginal(offset: Int) = grouped.take(offset).count { it != '.' }
        }
        return TransformedText(AnnotatedString(grouped), offsets)
    }
}
