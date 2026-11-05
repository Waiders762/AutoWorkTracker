package com.autoworktracker.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.autoworktracker.ui.theme.Blue
import com.autoworktracker.ui.theme.Emerald
import androidx.compose.material3.MaterialTheme

@Composable
fun GlowBackdrop(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    if (!dark) return
    Canvas(modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Emerald.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.92f, 0f),
                radius = size.width * 0.72f,
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.92f, 0f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Blue.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(0f, size.height * 0.18f),
                radius = size.width * 0.62f,
            ),
            radius = size.width * 0.62f,
            center = Offset(0f, size.height * 0.12f),
        )
    }
}

@Composable
fun PulsingDot(
    color: Color,
    pulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "dot-alpha",
    )
    Box(
        modifier
            .size(12.dp)
            .background(color.copy(alpha = alpha), CircleShape),
    )
}
