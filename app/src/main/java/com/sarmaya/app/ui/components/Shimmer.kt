package com.sarmaya.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    background(brush)
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.LightGray.copy(alpha = 0.1f), shape)
            .shimmerEffect()
    )
}

@Composable
fun ShimmerRow(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shimmerEffect()
    )
}
