package com.tekmoon.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tekmoon.designsystem.DsTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.tekmoon.designsystem.analytics.LocalAnalytics
import com.tekmoon.designsystem.foundation.DsColors
import com.tekmoon.designsystem.foundation.dsMinimumTouchTarget
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.platform.hapticClick
import com.tekmoon.designsystem.tokens.BorderTokens

// region size
private data class ButtonSizeSpec(
    val padding: PaddingValues,
    val textStyle: TextStyle,
    val iconSize: Dp,
    val minHeight: Dp
)

sealed class DsButtonSize {

    object Small : DsButtonSize()
    object Medium : DsButtonSize()
    object Large : DsButtonSize()

    data class Custom(
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val minHeight: Dp,
        val iconSize: Dp,
        val useBodyText: Boolean = false
    ) : DsButtonSize()
}

@Composable
private fun resolveButtonSizeSpec(
    size: DsButtonSize
): ButtonSizeSpec =
    when (size) {
        DsButtonSize.Small -> ButtonSizeSpec(
            padding = PaddingValues(
                horizontal = DsTheme.spacing.md,
                vertical = DsTheme.spacing.sm
            ),
            textStyle = DsTheme.typography.xs,
            iconSize = 14.dp,
            minHeight = 32.dp
        )

        DsButtonSize.Medium -> ButtonSizeSpec(
            padding = PaddingValues(
                horizontal = DsTheme.spacing.lg,
                vertical = DsTheme.spacing.md
            ),
            textStyle = DsTheme.typography.xs,
            iconSize = 16.dp,
            minHeight = 40.dp
        )

        DsButtonSize.Large -> ButtonSizeSpec(
            padding = PaddingValues(
                horizontal = DsTheme.spacing.xl,
                vertical = DsTheme.spacing.lg
            ),
            textStyle = DsTheme.typography.sm,
            iconSize = 18.dp,
            minHeight = 48.dp
        )

        is DsButtonSize.Custom -> ButtonSizeSpec(
            padding = PaddingValues(
                horizontal = size.horizontalPadding,
                vertical = size.verticalPadding
            ),
            textStyle = if (size.useBodyText)
                DsTheme.typography.sm
            else
                DsTheme.typography.xs,
            iconSize = size.iconSize,
            minHeight = size.minHeight
        )
    }
// endregion

// region ========= Definitions =========
enum class DsButtonVariant { Solid, Outlined, Text }
enum class DsButtonIntent { Primary, Secondary, Destructive }
enum class DsButtonLoadingMode { ReplaceContent, KeepText }
enum class DsButtonIconPosition { Start, End }

@Composable
private fun rememberButtonInteraction(): ButtonInteractionState {
    val interactionSource = remember { MutableInteractionSource() }

    return ButtonInteractionState(
        interactionSource = interactionSource,
        isPressed = interactionSource.collectIsPressedAsState().value,
        isHovered = interactionSource.collectIsHoveredAsState().value,
        isFocused = interactionSource.collectIsFocusedAsState().value
    )
}

private data class ButtonInteractionState(
    val interactionSource: MutableInteractionSource,
    val isPressed: Boolean,
    val isHovered: Boolean,
    val isFocused: Boolean
)

private data class ButtonVisualStyle(
    val background: Color,
    val content: Color,
    val border: Color?
)

private fun resolveButtonStyle(
    intent: DsButtonIntent,
    variant: DsButtonVariant,
    enabled: Boolean,
    colors: DsColors,
    overrides: ButtonColorOverrides
): ButtonVisualStyle {

    val background = overrides.background ?: when (variant) {
        DsButtonVariant.Solid -> when {
            !enabled                              -> colors.bgLight
            intent == DsButtonIntent.Primary      -> colors.primary
            intent == DsButtonIntent.Secondary    -> colors.bgLight
            intent == DsButtonIntent.Destructive  -> colors.danger
            else                                  -> colors.bgLight
        }

        DsButtonVariant.Outlined,
        DsButtonVariant.Text -> Color.Transparent

        else -> Color.Transparent
    }

    val content = overrides.content ?: when (variant) {
        DsButtonVariant.Solid -> when {
            !enabled                              -> colors.textMuted
            intent == DsButtonIntent.Primary      -> colors.onPrimary
            intent == DsButtonIntent.Secondary    -> colors.text
            intent == DsButtonIntent.Destructive  -> colors.onPrimary
            else                                  -> colors.text
        }

        DsButtonVariant.Outlined,
        DsButtonVariant.Text -> when {
            !enabled                              -> colors.textMuted
            intent == DsButtonIntent.Primary      -> colors.primary
            intent == DsButtonIntent.Secondary    -> colors.text
            intent == DsButtonIntent.Destructive  -> colors.danger
            else                                  -> colors.text
        }

        else -> colors.text
    }

    val border = overrides.border ?: when (variant) {
        DsButtonVariant.Outlined -> when {
            !enabled                              -> colors.textMuted
            intent == DsButtonIntent.Primary      -> colors.primary
            intent == DsButtonIntent.Secondary    -> colors.text
            intent == DsButtonIntent.Destructive  -> colors.danger
            else                                  -> colors.textMuted
        }

        DsButtonVariant.Solid,
        DsButtonVariant.Text -> null

        else -> null
    }

    return ButtonVisualStyle(
        background = background,
        content = content,
        border = border
    )
}

private fun applyInteractionOverlay(
    style: ButtonVisualStyle,
    interaction: ButtonInteractionState,
    enabled: Boolean
): ButtonVisualStyle {

    val alpha = when {
        !enabled -> 0.5f
        interaction.isPressed -> 0.85f
        interaction.isHovered -> 0.95f
        else -> 1f
    }

    return style.copy(
        background = if (style.background == Color.Transparent)
            Color.Transparent
        else
            style.background.copy(alpha = alpha),
        content = style.content.copy(alpha = alpha),
        border = style.border?.copy(alpha = alpha)
    )
}

private data class ButtonColorOverrides(
    val background: Color?,
    val content: Color?,
    val border: Color?
)
// endregion


// region ========== PUBLIC API ==========
@Composable
fun DsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,

    withHaptic: Boolean = false,

    size: DsButtonSize = DsButtonSize.Medium,

    enabled: Boolean = true,
    loading: Boolean = false,
    loadingMode: DsButtonLoadingMode = DsButtonLoadingMode.ReplaceContent,

    intent: DsButtonIntent = DsButtonIntent.Primary,
    variant: DsButtonVariant = DsButtonVariant.Solid,

    icon: DsImageSource? = null,
    iconPosition: DsButtonIconPosition = DsButtonIconPosition.Start,

    backgroundColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,

    /**
     * Stable identifier emitted with the `"ds_button_clicked"` analytics event when this
     * button is tapped. `null` (default) disables analytics for this instance — useful when
     * the click handler itself already emits a more meaningful event.
     */
    analyticsId: String? = null,
    /** Extra params merged into the analytics event payload alongside `id` + `text`. */
    analyticsParams: Map<String, Any?> = emptyMap(),
) {
    val interaction = rememberButtonInteraction()
    val colors = DsTheme.colors
    val analytics = LocalAnalytics.current

    val sizeSpec = resolveButtonSizeSpec(size)

    val baseStyle = resolveButtonStyle(
        intent,
        variant,
        enabled,
        colors,
        ButtonColorOverrides(backgroundColor, contentColor, borderColor)
    )

    val style = applyInteractionOverlay(baseStyle, interaction, enabled)

    val trackedClick: () -> Unit = if (analyticsId == null) onClick else {
        {
            analytics.track(
                event = "ds_button_clicked",
                params = mapOf(
                    "id" to analyticsId,
                    "text" to text,
                ) + analyticsParams,
            )
            onClick()
        }
    }

    ButtonContainer(
        modifier = modifier,
        style = style,
        enabled = enabled && !loading,
        withHaptic = withHaptic,
        onClick = trackedClick,
        padding = sizeSpec.padding,
        minHeight = sizeSpec.minHeight
    ) {
        when {
            loading && loadingMode == DsButtonLoadingMode.ReplaceContent ->
                DsButtonLoader(style.content, sizeSpec.iconSize)

            else ->
                DsButtonContent(
                    text = text,
                    icon = icon,
                    iconPosition = iconPosition,
                    color = style.content,
                    textStyle = sizeSpec.textStyle,
                    iconSize = sizeSpec.iconSize,
                    showLoader = loading && loadingMode == DsButtonLoadingMode.KeepText
                )
        }
    }
}
// endregion


@Composable
private fun ButtonContainer(
    modifier: Modifier,
    style: ButtonVisualStyle,
    minHeight: Dp,
    enabled: Boolean,
    padding: PaddingValues,
    withHaptic: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .dsMinimumTouchTarget(48.dp)
            .clip(DsTheme.shapes.lg)
            .background(style.background)
            .then(
                style.border?.let {
                    Modifier.border(
                        width = BorderTokens.Thin,
                        color = style.border,
                        shape = DsTheme.shapes.lg
                    )
                } ?: Modifier
            )
            .heightIn(min = minHeight)
            .focusable(enabled)
            .let {
                if (withHaptic) {
                    it.hapticClick(enabled = enabled, onClick = onClick)
                } else {
                    it.clickable(enabled = enabled, onClick = onClick)
                }
            }
            .semantics { role = Role.Button }
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


@Composable
private fun DsButtonContent(
    text: String,
    icon: DsImageSource? = null,
    iconPosition: DsButtonIconPosition,
    color: Color,
    textStyle: TextStyle,
    iconSize: Dp,
    showLoader: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {

        if (icon != null && iconPosition == DsButtonIconPosition.Start) {
            DsButtonIcon(icon, iconSize, color)
            Spacer(Modifier.width(DsTheme.spacing.sm))
        }

        DsText(
            text = text,
            color = color,
            style = textStyle,
            modifier = Modifier.alignByBaseline()
        )

        if (showLoader) {
            Spacer(Modifier.width(DsTheme.spacing.sm))
            DsButtonLoader(color, iconSize)
        }

        if (icon != null && iconPosition == DsButtonIconPosition.End) {
            Spacer(Modifier.width(DsTheme.spacing.sm))
            DsButtonIcon(icon, iconSize, color)
        }
    }
}


@Composable
private fun DsButtonLoader(color: Color, iconSize: Dp) {
    DsCircularProgress(
        modifier = Modifier
            .size(iconSize),
        componentSize = iconSize,
        color = color
    )
}

@Composable
private fun DsButtonIcon(
    source: DsImageSource,
    size: Dp,
    tint: Color
) {
    DsImage(
        source = source,
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = tint
    )
}