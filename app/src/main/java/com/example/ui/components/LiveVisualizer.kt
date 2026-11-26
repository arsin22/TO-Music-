package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val BAR_COUNT = 28

@Composable
fun LiveVisualizer(
    isPlaying: Boolean,
    accentHue: Int,
    playbackRate: Float = 1f,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, playbackRate) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    phase += dt * 4.8f * playbackRate
                }
                lastNanos = frameNanos
            }
        }
    }

    val bottomColor = Color.hsl(accentHue.toFloat(), 0.85f, 0.60f).copy(alpha = if (isPlaying) 0.78f else 0.30f)
    val topColor = Color.hsl(((accentHue + 32) % 360).toFloat(), 0.92f, 0.72f).copy(alpha = if (isPlaying) 0.95f else 0.35f)
    val brush = Brush.verticalGradient(colors = listOf(topColor, bottomColor))

    Canvas(
        modifier = modifier
            .width(260.dp)
            .height(48.dp)
    ) {
        val barWidth = (size.width / BAR_COUNT) * 0.48f
        val step = size.width / BAR_COUNT
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until BAR_COUNT) {
            val level = if (isPlaying) {
                val centerWeight = 1f - abs(i - BAR_COUNT / 2f) / (BAR_COUNT / 1.6f)
                val waveA = sin(phase + i * 0.42f)
                val waveB = cos(phase * 1.4f - i * 0.27f)
                (0.36f + (waveA * 0.30f + waveB * 0.22f) * (0.65f + centerWeight * 0.45f))
                    .coerceIn(0.14f, 0.95f)
            } else {
                (0.10f + 0.04f * sin(i * 0.5f)).coerceIn(0.06f, 0.20f)
            }

            val barHeight = size.height * level
            val x = i * step + (step - barWidth) / 2f
            val y = size.height - barHeight
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
        }
    }
}

@Composable
fun MiniEqualizer(
    isPlaying: Boolean,
    accentHue: Int,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    phase += dt * 6.5f
                }
                lastNanos = frameNanos
            }
        }
    }

    val barColor = Color.hsl(accentHue.toFloat(), 0.86f, 0.68f)
    val baseHeights = floatArrayOf(0.65f, 1.0f, 0.50f, 0.85f)

    Canvas(
        modifier = modifier
            .width(16.dp)
            .height(16.dp)
    ) {
        val barWidth = 2.5.dp.toPx()
        val gap = (size.width - barWidth * 4) / 3f
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until 4) {
            val hRatio = if (isPlaying) {
                val pulse = 0.45f + 0.55f * abs(sin(phase + i * 0.9f))
                (baseHeights[i] * pulse).coerceIn(0.22f, 1f)
            } else {
                0.25f
            }
            val h = size.height * hRatio
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = corner
            )
        }
    }
}
