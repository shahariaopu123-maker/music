package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonPink
import com.example.ui.theme.SoftPink
import com.example.ui.theme.WaveCyan
import kotlin.math.sin

@Composable
fun WaveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    maxHeight: Dp = 48.dp,
    barColorStart: Color = NeonPink,
    barColorEnd: Color = SoftPink
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.5f)).coerceAtLeast(3.dp.toPx())
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(barColorStart, barColorEnd, barColorStart.copy(alpha = 0.4f)),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until barCount) {
            val progressFactor = i.toFloat() / barCount.toFloat()
            val waveHeightRatio = if (isPlaying) {
                val wave1 = sin(phase + progressFactor * 5.0).toFloat() * 0.35f + 0.45f
                val wave2 = sin(phase * 1.6 - progressFactor * 3.5).toFloat() * 0.2f
                (wave1 + wave2).coerceIn(0.12f, 0.95f)
            } else {
                0.15f + sin(progressFactor * 3.14).toFloat() * 0.1f
            }

            val currentBarHeight = canvasHeight * waveHeightRatio
            val x = i * (barWidth + spacing)
            val y = (canvasHeight - currentBarHeight) / 2f

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
