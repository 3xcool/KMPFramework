package com.tekmoon.designsystem.image

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

internal object DsSkeletonDefaults {
    val shape: Shape = RoundedCornerShape(0.dp)
}

@Composable
fun DsShimmer(
    modifier: Modifier,
    shape: Shape,
    baseColor: Color = Color(0xFFE0E0E0),
    highlightColor: Color = Color(0xFFF5F5F5)
) {
    val motion = LocalDsMotion.current

    if (motion == DsMotionLevel.Reduced) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(baseColor)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "skeleton")

    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "skeletonTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}


@Preview(name = "Skeleton – Rectangle")
@Composable
private fun SkeletonRectanglePreview() {
    DsShimmer(
        modifier = Modifier.size(200.dp, 120.dp),
        shape = RoundedCornerShape(0.dp)
    )
}

@Preview(name = "Skeleton – Rounded")
@Composable
private fun SkeletonRoundedPreview() {
    DsShimmer(
        modifier = Modifier.size(200.dp, 120.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

@Preview(name = "Skeleton – Circle")
@Composable
private fun SkeletonCirclePreview() {
    DsShimmer(
        modifier = Modifier.size(64.dp),
        shape = CircleShape
    )
}