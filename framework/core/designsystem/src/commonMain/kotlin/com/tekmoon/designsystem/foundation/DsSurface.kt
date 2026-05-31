package com.tekmoon.designsystem.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.tekmoon.designsystem.DsTheme

enum class DsSurfaceRole {
    Flat,
    Card,
    Floating,
    Modal,
    Pressed
}



/**
 * Attention
 * Shadow → outside clip
 * Clip → defines bounds
 * Ripple → inside clip
 * Background + content → inside clip
 *
 *
 *
 * DsSurface(
 *     role = DsSurfaceRole.Card,
 *     ripple = DsRipple.Bounded(
 *         color = DsTheme.colors.primary.copy(alpha = 0.12f)
 *     ),
 *     onClick = { }
 * ) {
 *     Text("Shadow + ripple")
 * }
 * or
 * DsSurface(
 *     role = DsSurfaceRole.Floating,
 *     ripple = DsRipple.Unbounded(),
 *     onClick = { }
 * ) {
 *     Icon(...)
 * }
 */

@Composable
fun DsSurface(
    modifier: Modifier = Modifier,
    role: DsSurfaceRole = DsSurfaceRole.Flat,
    shape: Shape = DsTheme.shapes.surface,
    enabled: Boolean = true,
    ripple: DsRipple = DsRipple.None,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val shadow = resolveSurfaceShadow(
        role = role,
        isPressed = isPressed,
        isHovered = isHovered
    )

    val indication = dsRippleIndication(ripple)

    val clickableModifier =
        if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = indication,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            )
        } else {
            Modifier
        }

    val backgroundColor = when (role) {
        DsSurfaceRole.Flat -> DsTheme.surfaceColors.flat
        DsSurfaceRole.Card -> DsTheme.surfaceColors.raised
        DsSurfaceRole.Floating -> DsTheme.surfaceColors.floating
        DsSurfaceRole.Modal -> DsTheme.surfaceColors.modal
        DsSurfaceRole.Pressed -> DsTheme.surfaceColors.raised
    }

    Box(
        modifier
            // Pay attention to this order
            .then(
                if (shadow != null)
                    Modifier.dsShadow(shadow, shape)
                else Modifier
            )
            .clip(shape)
            .background(backgroundColor)
            .then(clickableModifier)
    ) {
        content()
    }
}


@Composable
private fun resolveSurfaceShadow(
    role: DsSurfaceRole,
    isPressed: Boolean,
    isHovered: Boolean
): DsShadowSpec? = when {
    isPressed -> DsTheme.shadows.innerPressed
    isHovered -> DsTheme.shadows.outerHover

    role == DsSurfaceRole.Card -> DsTheme.shadows.outerRest
    role == DsSurfaceRole.Floating -> DsTheme.shadows.outerHover
    role == DsSurfaceRole.Modal -> DsTheme.shadows.outerModal

    else -> null
}

@Immutable
data class DsSurfaceColors(
    val flat: Color,
    val raised: Color,
    val floating: Color,
    val modal: Color
)

internal val LocalDsSurfaceColors =
    staticCompositionLocalOf<DsSurfaceColors> {
        error("DsSurfaceColors not provided")
    }