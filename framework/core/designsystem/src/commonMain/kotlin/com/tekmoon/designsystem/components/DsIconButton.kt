package com.tekmoon.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.foundation.DsColors
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.tokens.BorderTokens

// ─── Size ────────────────────────────────────────────────────────────────────

private data class IconButtonSizeSpec(
    val touchTarget: Dp,  // overall tappable area (min 44 dp per accessibility guidelines)
    val iconSize: Dp,
    val shape: @Composable () -> Shape
)

sealed class DsIconButtonSize {
    object Small  : DsIconButtonSize()   // 36 dp touch, 16 dp icon
    object Medium : DsIconButtonSize()   // 44 dp touch, 20 dp icon
    object Large  : DsIconButtonSize()   // 52 dp touch, 24 dp icon
}

@Composable
private fun resolveSizeSpec(size: DsIconButtonSize): IconButtonSizeSpec = when (size) {
    DsIconButtonSize.Small  -> IconButtonSizeSpec(36.dp, 16.dp) { DsTheme.shapes.md }
    DsIconButtonSize.Medium -> IconButtonSizeSpec(44.dp, 20.dp) { DsTheme.shapes.lg }
    DsIconButtonSize.Large  -> IconButtonSizeSpec(52.dp, 24.dp) { DsTheme.shapes.lg }
}

// ─── Public API ──────────────────────────────────────────────────────────────

/**
 * An icon-only tappable button backed by the DsButton visual token system.
 *
 * Supports all [DsButtonVariant] and [DsButtonIntent] combinations, including
 * the new [DsButtonIntent.Destructive].
 *
 * Minimum touch target is always ≥ 44 dp (Medium/Large) or 36 dp (Small) —
 * wrap in a larger container if stricter accessibility compliance is needed.
 *
 * @param icon               The icon to display.
 * @param contentDescription Accessibility label for the button.
 * @param onClick            Click callback.
 * @param modifier           Applied to the outermost touch-target container.
 * @param size               Controls touch-target and icon dimensions.
 * @param intent             Semantic color intent (Primary / Secondary / Destructive).
 * @param variant            Visual style (Solid / Outlined / Text).
 * @param enabled            Whether the button responds to interaction.
 * @param backgroundColor    Optional override for the background color.
 * @param contentColor       Optional override for the icon tint.
 * @param borderColor        Optional override for the border color (Outlined only).
 */
@Composable
fun DsIconButton(
    icon: DsImageSource,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DsIconButtonSize = DsIconButtonSize.Medium,
    intent: DsButtonIntent = DsButtonIntent.Primary,
    variant: DsButtonVariant = DsButtonVariant.Text,
    enabled: Boolean = true,
    backgroundColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val sizeSpec = resolveSizeSpec(size)
    val shape    = sizeSpec.shape()
    val colors   = DsTheme.colors

    val baseStyle = resolveIconButtonStyle(intent, variant, enabled, colors,
        backgroundColor, contentColor, borderColor)

    val alpha = when {
        !enabled   -> 0.5f
        isPressed  -> 0.80f
        isHovered  -> 0.92f
        else       -> 1f
    }
    val style = baseStyle.copy(
        background = if (baseStyle.background == Color.Transparent) Color.Transparent
                     else baseStyle.background.copy(alpha = alpha),
        iconTint   = baseStyle.iconTint.copy(alpha = alpha),
        border     = baseStyle.border?.copy(alpha = alpha),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeSpec.touchTarget)
            .clip(shape)
            .background(style.background)
            .then(
                style.border?.let {
                    Modifier.border(BorderTokens.Thin, it, shape)
                } ?: Modifier
            )
            .focusable(enabled, interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { role = Role.Button }
    ) {
        DsImage(
            source = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(sizeSpec.iconSize),
            tint = style.iconTint,
        )
    }
}

// ─── Internal ────────────────────────────────────────────────────────────────

private data class IconButtonStyle(
    val background: Color,
    val iconTint: Color,
    val border: Color?,
)

private fun resolveIconButtonStyle(
    intent: DsButtonIntent,
    variant: DsButtonVariant,
    enabled: Boolean,
    colors: DsColors,
    backgroundOverride: Color?,
    contentOverride: Color?,
    borderOverride: Color?,
): IconButtonStyle {
    val background = backgroundOverride ?: when (variant) {
        DsButtonVariant.Solid -> when {
            !enabled                             -> colors.bgLight
            intent == DsButtonIntent.Primary     -> colors.primary
            intent == DsButtonIntent.Secondary   -> colors.bgLight
            intent == DsButtonIntent.Destructive -> colors.danger
            else                                 -> colors.bgLight
        }
        DsButtonVariant.Outlined,
        DsButtonVariant.Text -> Color.Transparent
        else                 -> Color.Transparent
    }

    val iconTint = contentOverride ?: when (variant) {
        DsButtonVariant.Solid -> when {
            !enabled                             -> colors.textMuted
            intent == DsButtonIntent.Primary     -> colors.onPrimary
            intent == DsButtonIntent.Secondary   -> colors.text
            intent == DsButtonIntent.Destructive -> colors.onPrimary
            else                                 -> colors.text
        }
        DsButtonVariant.Outlined,
        DsButtonVariant.Text -> when {
            !enabled                             -> colors.textMuted
            intent == DsButtonIntent.Primary     -> colors.primary
            intent == DsButtonIntent.Secondary   -> colors.text
            intent == DsButtonIntent.Destructive -> colors.danger
            else                                 -> colors.text
        }
        else -> colors.text
    }

    val border = borderOverride ?: when (variant) {
        DsButtonVariant.Outlined -> when {
            !enabled                             -> colors.textMuted
            intent == DsButtonIntent.Primary     -> colors.primary
            intent == DsButtonIntent.Secondary   -> colors.text
            intent == DsButtonIntent.Destructive -> colors.danger
            else                                 -> colors.textMuted
        }
        else -> null
    }

    return IconButtonStyle(background, iconTint, border)
}
