package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder

/**
 * Modifier to provide visible focus feedback on Android TV when navigating with a D-Pad remote.
 * When focused, applies an accent glowing border around the element.
 */
fun Modifier.dpadFocusable(
    shape: Shape,
    focusedBorderColor: Color = AeroCyan,
    unfocusedBorderColor: Color = CardBorder,
    focusedBorderWidth: Dp = 2.dp,
    unfocusedBorderWidth: Dp = 1.dp
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "dpadFocusBorder"
    )
    val borderWidth = if (isFocused) focusedBorderWidth else unfocusedBorderWidth

    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .border(borderWidth, borderColor, shape)
}
