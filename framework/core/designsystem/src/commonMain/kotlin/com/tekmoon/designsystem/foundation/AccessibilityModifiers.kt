package com.tekmoon.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expands the node's tap region to at least [size] on each axis without enlarging its painted
 * content. The visible content is centered inside the reserved slot, so the component looks
 * identical but every pixel inside the 48dp box (the WCAG / Material baseline) hits this node.
 *
 * Apply on the clickable surface itself, not the visual element inside it.
 */
fun Modifier.dsMinimumTouchTarget(size: Dp = 48.dp): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val minPx = size.roundToPx()
        val w = maxOf(placeable.width, minPx)
        val h = maxOf(placeable.height, minPx)
        layout(w, h) {
            placeable.place(
                x = (w - placeable.width) / 2,
                y = (h - placeable.height) / 2,
            )
        }
    }
