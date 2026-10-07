package com.example.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.flux.ui.theme.UISurface

@Composable
fun FluxCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, com.example.flux.ui.theme.UIBorder.copy(alpha = .6f), RoundedCornerShape(24.dp))
            .background(UISurface),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}
