package com.tekmoon.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme

object DsProgressDefaults {
    val Size = 16.dp          // button-friendly
    val Stroke = 2.dp
}

@Composable
fun DsCircularProgress(
    modifier: Modifier = Modifier,
    componentSize: Dp = DsProgressDefaults.Size,
    strokeWidth: Dp = DsProgressDefaults.Stroke,
    color: Color = DsTheme.colors.content,
) {
    val transition = rememberInfiniteTransition(label = "ds-progress")

    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing)
        ),
        label = "rotation"
    )


    Canvas(
        modifier = modifier
            .size(componentSize)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
    ) {

        val strokePx = strokeWidth.toPx()
        val diameter = size.minDimension - strokePx

        drawArc(
            color = color,
            startAngle = rotation.value,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
            topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            ),
            size = androidx.compose.ui.geometry.Size(diameter, diameter)
        )
    }
}
