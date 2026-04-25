package com.tekmoon.designsystem.foundation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.max

/* -------------------------------------------------------------------------- */
/* Platform blur bridge                                                        */
/* -------------------------------------------------------------------------- */

expect fun Paint.applyPlatformBlur(radiusPx: Float)

/* -------------------------------------------------------------------------- */
/* Caches                                                                      */
/* -------------------------------------------------------------------------- */

@Stable
private class CachedOutline(
    val shape: Shape
) {
    var size: Size = Size.Zero
    var outline: Outline? = null
}

@Stable
private class CachedShadowPaint {
    var blurPx: Float = -1f
    var color: Color = Color.Unspecified
    val paint: Paint = Paint()
}

/* -------------------------------------------------------------------------- */
/* Public modifier                                                             */
/* -------------------------------------------------------------------------- */

fun Modifier.dsShadow(
    spec: DsShadowSpec,
    shape: Shape
): Modifier = composed {

    val outlineCache = remember { CachedOutline(shape) }
    val paintCache = remember { CachedShadowPaint() }

    drawBehind {
        // Update outline cache
        if (outlineCache.size != size) {
            outlineCache.size = size
            outlineCache.outline =
                shape.createOutline(size, layoutDirection, this)
        }

        val outerOutline = outlineCache.outline ?: return@drawBehind
        val paint = rememberShadowPaint(spec, paintCache)

        if (spec.inset) {
            drawInnerShadow(
                spec = spec,
                shape = shape,              // ✅ pass Shape
                outerOutline = outerOutline,
                paint = paint
            )
        } else {
            drawOuterShadow(
                spec = spec,
                outline = outerOutline,
                paint = paint
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Paint resolution                                                            */
/* -------------------------------------------------------------------------- */

private fun DrawScope.rememberShadowPaint(
    spec: DsShadowSpec,
    cache: CachedShadowPaint
): Paint {
    val blurPx = spec.blur.toPx()

    if (cache.blurPx != blurPx || cache.color != spec.color) {
        cache.blurPx = blurPx
        cache.color = spec.color

        cache.paint.apply {
            color = spec.color
            isAntiAlias = true
            applyPlatformBlur(blurPx)
        }
    }

    return cache.paint
}

/* -------------------------------------------------------------------------- */
/* Outer shadow                                                                */
/* -------------------------------------------------------------------------- */

private fun DrawScope.drawOuterShadow(
    spec: DsShadowSpec,
    outline: Outline,
    paint: Paint
) {
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(
            spec.offsetX.toPx(),
            spec.offsetY.toPx()
        )
        canvas.drawOutline(outline, paint)
        canvas.restore()
    }
}

/* -------------------------------------------------------------------------- */
/* Inner (inset) shadow                                                        */
/* -------------------------------------------------------------------------- */

private fun DrawScope.drawInnerShadow(
    spec: DsShadowSpec,
    shape: Shape,
    outerOutline: Outline,
    paint: Paint
) {
    val blurPx = max(spec.blur.toPx(), 0.1f)
    val directionOffset = spec.direction.offset(blurPx)

    drawIntoCanvas { canvas ->
        // Isolated layer
        canvas.saveLayer(Rect(Offset.Zero, size), Paint())

        // Draw shadow
        canvas.save()
        canvas.translate(directionOffset.x, directionOffset.y)
        canvas.drawOutline(outerOutline, paint)
        canvas.restore()

        // Clear inner area
        val clearPaint = Paint().apply {
            blendMode = BlendMode.Clear
        }

        val inset = blurPx
        val innerSize = Size(
            size.width - inset * 2,
            size.height - inset * 2
        )

        canvas.save()
        canvas.translate(inset, inset)

        // Re-create inner outline from SHAPE (not Outline)
        val innerOutline = shape.createOutline(
            innerSize,
            layoutDirection,
            this
        )

        canvas.drawOutline(innerOutline, clearPaint)
        canvas.restore()

        // Restore layer
        canvas.restore()
    }
}


/* -------------------------------------------------------------------------- */
/* Animation helper                                                            */
/* -------------------------------------------------------------------------- */

@Composable
fun animateShadow(
    target: DsShadowSpec,
    animationSpec: FiniteAnimationSpec<Float> = tween(150)
): DsShadowSpec {
    val blur by animateFloatAsState(
        targetValue = target.blur.value,
        animationSpec = animationSpec
    )

    val offsetY by animateFloatAsState(
        targetValue = target.offsetY.value,
        animationSpec = animationSpec
    )

    return target.copy(
        blur = blur.dp,
        offsetY = offsetY.dp
    )
}
