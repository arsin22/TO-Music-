package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.LocalAppColors
import java.io.File

enum class ArtworkSize(val dp: Dp, val corner: Dp, val iconDp: Dp) {
    SM(46.dp, 12.dp, 22.dp),
    MD(56.dp, 16.dp, 26.dp),
    LG(256.dp, 26.dp, 90.dp)
}

fun resolveCoverModel(coverFilePath: String?): Any? {
    if (coverFilePath.isNullOrBlank()) return null
    return if (coverFilePath.startsWith("content://") || coverFilePath.startsWith("file://")) {
        Uri.parse(coverFilePath)
    } else {
        File(coverFilePath)
    }
}

@Composable
fun TrackArtwork(
    title: String,
    coverFilePath: String?,
    accentHue: Int,
    size: ArtworkSize = ArtworkSize.MD,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val pxSize = with(LocalDensity.current) { size.dp.roundToPx().coerceAtLeast(64) }
    val shape = remember(size.corner) { RoundedCornerShape(size.corner) }
    val coverModel = remember(coverFilePath) { resolveCoverModel(coverFilePath) }
    var imageError by remember(coverFilePath) { mutableStateOf(false) }

    val imageRequest = remember(coverModel, pxSize, context) {
        coverModel?.let { model ->
            ImageRequest.Builder(context)
                .data(model)
                .size(pxSize, pxSize)
                .allowHardware(true)
                .crossfade(false)
                .build()
        }
    }

    val showFallback = imageRequest == null || imageError

    val deepBase = remember(accentHue, colors.isDark) {
        Color.hsl(
            accentHue.toFloat(),
            if (colors.isDark) 0.45f else 0.38f,
            if (colors.isDark) 0.16f else 0.86f
        ).copy(alpha = 0.96f)
    }
    val baseEnd = if (colors.isDark) Color(0xFF09090B) else Color(0xFFDCE4F2)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(deepBase)
            .border(1.dp, colors.glassBorderStrong, shape)
    ) {
        if (showFallback) {
            val topGlow = Color.hsl(
                accentHue.toFloat(),
                if (colors.isDark) 0.82f else 0.72f,
                if (colors.isDark) 0.62f else 0.66f
            ).copy(alpha = if (colors.isDark) 0.42f else 0.35f)

            val rotation = if (isPlaying) {
                val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 9000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "disc_angle"
                )
                angle
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(deepBase, baseEnd),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(topGlow, Color.Transparent),
                            center = Offset(0.25f * pxSize, 0.2f * pxSize),
                            radius = pxSize * 0.9f
                        )
                    )
            )
            Icon(
                imageVector = Icons.Rounded.Album,
                contentDescription = title,
                tint = if (colors.isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF1E293B).copy(alpha = 0.78f),
                modifier = Modifier
                    .size(size.iconDp)
                    .rotate(rotation)
            )
        } else {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                onError = { imageError = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
