package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderNeon
import com.example.ui.theme.NeonPink

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0x18FFFFFF),
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x40FF4FA3),
            Color(0x20FFFFFF),
            Color(0x10FF4FA3)
        )
    ),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderBrush, shape)

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color(0x221B1828),
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x55FF4FA3),
            Color(0x25FFFFFF),
            Color(0x15FF4FA3)
        )
    ),
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderBrush, shape)
            .then(clickableModifier)
    ) {
        content()
    }
}

@Composable
fun NeonGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = NeonPink,
    contentColor: Color = Color(0xFF08080C)
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = NeonPink,
                spotColor = NeonPink
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        NeonPink,
                        Color(0xFFFF70B8)
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        // Content will be supplied where used
    }
}
