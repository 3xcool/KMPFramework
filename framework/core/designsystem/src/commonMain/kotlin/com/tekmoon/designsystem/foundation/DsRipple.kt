package com.tekmoon.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.CoroutineScope

/**
 * No ripple
 *
 * Bounded ripple (inside shape)
 *
 * Unbounded ripple (buttons / FABs)
 */

sealed interface DsRipple {
    data object None : DsRipple

    data class Bounded(
        val color: Color,
        val radius: Dp = Dp.Unspecified
    ) : DsRipple

    data class Unbounded(
        val color: Color,
        val radius: Dp = Dp.Unspecified
    ) : DsRipple
}

fun dsRippleIndication(ripple: DsRipple): Indication? =
    if (ripple == DsRipple.None) null
    else DsRippleNodeFactory(ripple)

private class DsRippleNodeFactory(
    private val ripple: DsRipple
) : IndicationNodeFactory {

    override fun create(
        interactionSource: InteractionSource
    ): Modifier.Node =
        DsRippleNode(
            ripple = ripple,
            interactionSource = interactionSource
        )

    override fun equals(other: Any?) =
        other is DsRippleNodeFactory && other.ripple == ripple

    override fun hashCode(): Int = ripple.hashCode()
}


private class DsRippleNode(
    private val ripple: DsRipple,
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private val radiusAnim = Animatable(0f)
    private val alphaAnim = Animatable(0f)

    private lateinit var scope: CoroutineScope

    override fun onAttach() {
        scope = coroutineScope

        scope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        scope.launch {
                            alphaAnim.snapTo(0.18f)
                            radiusAnim.animateTo(1f, tween(250))
                        }
                    }

                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> {
                        scope.launch {
                            alphaAnim.animateTo(0f, tween(200))
                            radiusAnim.snapTo(0f)
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val alpha = alphaAnim.value
        if (alpha <= 0f) return

        val maxRadius =
            when (ripple) {
                is DsRipple.Unbounded -> size.maxDimension
                is DsRipple.Bounded -> size.minDimension
                else -> return
            }

        drawCircle(
            color = when (ripple) {
                is DsRipple.Bounded -> ripple.color
                is DsRipple.Unbounded -> ripple.color
                else -> Color.Transparent
            },
            radius = maxRadius * radiusAnim.value,
            alpha = alpha,
            center = center
        )
    }
}


// ios didn't work
//
//@Composable
//private fun rememberRippleState(): Pair<Float, Float> {
//    val radius = remember { Animatable(0f) }
//    val alpha = remember { Animatable(0f) }
//    val scope = rememberCoroutineScope()
//
//    return radius.value to alpha.value
//}
//
//fun Modifier.manualRipple(
//    enabled: Boolean,
//    color: Color,
//    interactionSource: InteractionSource
//): Modifier = composed {
//
//    val radius = remember { Animatable(0f) }
//    val alpha = remember { Animatable(0f) }
//    val scope = rememberCoroutineScope()
//
//    LaunchedEffect(interactionSource) {
//        interactionSource.interactions.collect { interaction ->
//            when (interaction) {
//                is PressInteraction.Press -> {
//                    scope.launch {
//                        alpha.snapTo(0.25f)
//                        radius.animateTo(1f, tween(250))
//                    }
//                }
//                is PressInteraction.Release,
//                is PressInteraction.Cancel -> {
//                    scope.launch {
//                        alpha.animateTo(0f, tween(150))
//                        radius.snapTo(0f)
//                    }
//                }
//            }
//        }
//    }
//
//    drawWithContent {
//        drawContent()
//
//        if (alpha.value > 0f) {
//            drawCircle(
//                color = color,
//                radius = size.minDimension * radius.value,
//                alpha = alpha.value,
//                center = center
//            )
//        }
//    }
//}
//val rippleModifier =
//    if (ripple != DsRipple.None) {
//        Modifier.manualRipple(
//            enabled = enabled,
//            color = when (ripple) {
//                is DsRipple.Bounded -> ripple.color
//                is DsRipple.Unbounded -> ripple.color
//                else -> Color.Transparent
//            },
//            interactionSource = interactionSource
//        )
//    } else {
//        Modifier
//    }