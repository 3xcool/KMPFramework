package com.tekmoon.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.analytics.LocalAnalytics
import com.tekmoon.designsystem.foundation.dsMinimumTouchTarget
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageSource

// ─── Public API ──────────────────────────────────────────────────────────────

enum class DsBannerType { Info, Success, Warning, Danger }

/**
 * A single labeled action on a [DsBanner].
 *
 * The visible [label] doubles as the accessible name (DsButton sets the right
 * semantics), so no separate contentDescription is required.
 */
data class DsBannerAction(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * Full-width prominent banner — used to surface a state that needs the user's
 * attention and (usually) a response. Compared to [DsAlert] this component is
 * louder (stronger tint, accent stripe, action buttons inline) and assumes
 * placement at the top of a screen or section.
 *
 * @param title                     Bold header line.
 * @param message                   Supporting body text.
 * @param type                      Semantic intent — controls accent + tint colors.
 * @param primaryAction             The main call-to-action. Always rendered.
 * @param modifier                  Applied to the outermost container.
 * @param icon                      Optional leading icon, tinted with the accent color.
 * @param secondaryAction           Optional second action rendered to the left of [primaryAction]
 *                                  as a Text-variant button.
 * @param onDismiss                 When non-null, a dismiss ("×") button is shown in the corner.
 * @param dismissContentDescription Accessibility label for the dismiss button.
 *                                  Required (non-null) when [onDismiss] is non-null.
 */
@Composable
fun DsBanner(
    title: String,
    message: String,
    type: DsBannerType,
    primaryAction: DsBannerAction,
    modifier: Modifier = Modifier,
    icon: DsImageSource? = null,
    secondaryAction: DsBannerAction? = null,
    onDismiss: (() -> Unit)? = null,
    dismissContentDescription: String? = null,
    /**
     * Stable identifier emitted with `"ds_banner_primary_clicked"` when the primary action
     * fires. `null` (default) disables analytics for the primary action.
     */
    primaryAnalyticsId: String? = null,
    /**
     * Stable identifier emitted with `"ds_banner_secondary_clicked"` when the secondary action
     * fires. Ignored if [secondaryAction] is `null`.
     */
    secondaryAnalyticsId: String? = null,
    /**
     * Stable identifier emitted with `"ds_banner_dismissed"` when the dismiss X is tapped.
     * Ignored if [onDismiss] is `null`.
     */
    dismissAnalyticsId: String? = null,
    /**
     * Extra params merged into every event this banner emits (alongside each event's `id`
     * + `type`). Use for screen / module / user-state tags that apply to all three targets.
     */
    analyticsParams: Map<String, Any?> = emptyMap(),
) {
    require(onDismiss == null || dismissContentDescription != null) {
        "DsBanner: dismissContentDescription is required when onDismiss is non-null"
    }
    val colors = resolveBannerColors(type)
    val analytics = LocalAnalytics.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DsTheme.shapes.md)
            .background(colors.background)
            // IntrinsicSize.Min lets the leading stripe fill the height the rest of the
            // Row naturally takes (driven by the Column's intrinsic content).
            .height(IntrinsicSize.Min),
    ) {
        // Leading accent stripe — 4dp wide bar in the type's strong color.
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(colors.accent)
        )

        Column(
            modifier = Modifier
                .padding(DsTheme.spacing.md)
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (icon != null) {
                    DsImage(
                        source = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp),
                        tint = colors.accent,
                    )
                    Spacer(Modifier.width(DsTheme.spacing.sm))
                }

                Column(modifier = Modifier.weight(1f)) {
                    DsText(
                        text = title,
                        style = DsTheme.typography.md.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.content,
                    )
                    Spacer(Modifier.height(2.dp))
                    DsText(
                        text = message,
                        style = DsTheme.typography.sm,
                        color = colors.content,
                    )
                }

                if (onDismiss != null) {
                    val description = dismissContentDescription!!
                    val trackedDismiss: () -> Unit = if (dismissAnalyticsId == null) onDismiss else {
                        {
                            analytics.track(
                                event = "ds_banner_dismissed",
                                params = mapOf(
                                    "id" to dismissAnalyticsId,
                                    "type" to type.name,
                                ) + analyticsParams,
                            )
                            onDismiss()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .dsMinimumTouchTarget(48.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = trackedDismiss,
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = description
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        DsText(
                            text = "×",
                            style = DsTheme.typography.lg,
                            color = colors.content.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(DsTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (secondaryAction != null) {
                    val trackedSecondary: () -> Unit =
                        if (secondaryAnalyticsId == null) secondaryAction.onClick else {
                            {
                                analytics.track(
                                    event = "ds_banner_secondary_clicked",
                                    params = mapOf(
                                        "id" to secondaryAnalyticsId,
                                        "type" to type.name,
                                        "label" to secondaryAction.label,
                                    ) + analyticsParams,
                                )
                                secondaryAction.onClick()
                            }
                        }
                    // Text-variant secondary uses the banner's accent color so it stays in
                    // the same color family as the surrounding tint — better readability
                    // than the theme's default secondary text color, which can wash out
                    // against a tinted background.
                    DsButton(
                        text = secondaryAction.label,
                        onClick = trackedSecondary,
                        variant = DsButtonVariant.Text,
                        size = DsButtonSize.Small,
                        contentColor = colors.accent,
                    )
                    Spacer(Modifier.width(DsTheme.spacing.sm))
                }
                val trackedPrimary: () -> Unit =
                    if (primaryAnalyticsId == null) primaryAction.onClick else {
                        {
                            analytics.track(
                                event = "ds_banner_primary_clicked",
                                params = mapOf(
                                    "id" to primaryAnalyticsId,
                                    "type" to type.name,
                                    "label" to primaryAction.label,
                                ) + analyticsParams,
                            )
                            primaryAction.onClick()
                        }
                    }
                // Primary uses the banner's accent as the solid background with a
                // forced light/dark contrast-aware text color, instead of the theme's
                // global primary (which is blue regardless of banner type and clashes
                // visually on Warning/Success).
                DsButton(
                    text = primaryAction.label,
                    onClick = trackedPrimary,
                    variant = DsButtonVariant.Solid,
                    size = DsButtonSize.Small,
                    backgroundColor = colors.accent,
                    contentColor = colors.onAccent,
                )
            }
        }
    }
}

// ─── Internal ────────────────────────────────────────────────────────────────

private data class BannerColors(
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val content: Color,
)

@Composable
private fun resolveBannerColors(type: DsBannerType): BannerColors {
    val accent = when (type) {
        DsBannerType.Info    -> DsTheme.colors.info
        DsBannerType.Success -> DsTheme.colors.success
        DsBannerType.Warning -> DsTheme.colors.warning
        DsBannerType.Danger  -> DsTheme.colors.danger
    }
    // Pick black or white text against the accent based on perceptual luminance —
    // the Warning hue is light enough that white text falls below AA contrast.
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    return BannerColors(
        accent     = accent,
        onAccent   = onAccent,
        // Stronger tint than DsAlert (0.15) — banners need more visual weight.
        background = accent.copy(alpha = 0.20f),
        content    = DsTheme.textColors.primary,
    )
}
