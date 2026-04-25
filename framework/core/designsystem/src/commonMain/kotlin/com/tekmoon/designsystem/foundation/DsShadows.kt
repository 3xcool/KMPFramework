package com.tekmoon.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.tokens.ShadowSpec
import com.tekmoon.designsystem.tokens.ShadowTokens

@Immutable
data class DsShadowSpec(
    val color: Color,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val blur: Dp,
    val inset: Boolean = false,
    val direction: DsShadowDirection = DsShadowDirection.Center
)


@Immutable
data class DsShadows(
    val outerRest: DsShadowSpec,
    val outerHover: DsShadowSpec,
    val outerModal: DsShadowSpec,

    val innerPressed: DsShadowSpec,
    val innerInset: DsShadowSpec
)

fun ShadowSpec.toOuter(color: Color) = DsShadowSpec(
    color = color.copy(alpha = alpha),
    offsetY = offsetY.dp,
    blur = blur.dp
)

fun ShadowSpec.toInner(color: Color) = DsShadowSpec(
    color = color.copy(alpha = alpha),
    offsetY = (-offsetY).dp,
    blur = blur.dp,
    inset = true
)

val DefaultShadows = DsShadows(
    outerRest = ShadowTokens.Small.toOuter(Color.Black),
    outerHover = ShadowTokens.Medium.toOuter(Color.Black),
    outerModal = ShadowTokens.Large.toOuter(Color.Black),

    innerPressed = ShadowTokens.Small.toInner(Color.Black),
    innerInset = ShadowTokens.Medium.toInner(Color.Black)
)

enum class DsShadowDirection {
    Center,
    Top,
    Bottom,
    Left,
    Right
}

internal fun DsShadowDirection.offset(
    blurPx: Float
): Offset = when (this) {
    DsShadowDirection.Top -> Offset(0f, -blurPx * 0.6f)
    DsShadowDirection.Bottom -> Offset(0f, blurPx * 0.6f)
    DsShadowDirection.Left -> Offset(-blurPx * 0.6f, 0f)
    DsShadowDirection.Right -> Offset(blurPx * 0.6f, 0f)
    DsShadowDirection.Center -> Offset.Zero
}


