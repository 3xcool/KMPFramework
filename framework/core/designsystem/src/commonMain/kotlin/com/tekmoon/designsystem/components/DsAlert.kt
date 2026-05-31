package com.tekmoon.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.analytics.LocalAnalytics
import com.tekmoon.designsystem.foundation.dsMinimumTouchTarget
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageSource

// ─── Public API ──────────────────────────────────────────────────────────────

enum class AlertType { Danger, Warning, Success, Info }

/**
 * Inline alert banner — used to surface contextual feedback within a screen.
 *
 * @param message                   Primary message shown in the alert body.
 * @param type                      Semantic intent — controls colors and the default icon tint.
 * @param modifier                  Applied to the outermost container.
 * @param title                     Optional bold title rendered above [message].
 * @param icon                      Optional leading icon. Pass `null` to omit.
 * @param onDismiss                 When non-null a dismiss ("×") button is shown at the trailing edge.
 * @param dismissContentDescription Accessibility label for the dismiss button. Required (non-null)
 *                                  whenever [onDismiss] is non-null.
 */
@Composable
fun DsAlert(
    message: String,
    type: AlertType,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: DsImageSource? = null,
    onDismiss: (() -> Unit)? = null,
    dismissContentDescription: String? = null,
    /**
     * Stable identifier emitted with the `"ds_alert_dismissed"` analytics event when the
     * dismiss X is tapped. `null` (default) disables analytics for the dismiss action.
     * Ignored if [onDismiss] is `null` (no dismiss button rendered).
     */
    dismissAnalyticsId: String? = null,
    /** Extra params merged into the analytics event payload alongside `id` + `type`. */
    analyticsParams: Map<String, Any?> = emptyMap(),
) {
    require(onDismiss == null || dismissContentDescription != null) {
        "DsAlert: dismissContentDescription is required when onDismiss is non-null"
    }
    val colors = resolveAlertColors(type)
    val analytics = LocalAnalytics.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DsTheme.shapes.md)
            .background(colors.background)
            .padding(horizontal = DsTheme.spacing.md, vertical = DsTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DsTheme.spacing.sm)
    ) {
        // Leading icon
        if (icon != null) {
            DsImage(
                source = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp),
                tint = colors.content
            )
        }

        // Text block
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                DsText(
                    text = title,
                    style = DsTheme.typography.sm.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = colors.content
                )
            }
            DsText(
                text = message,
                style = DsTheme.typography.sm,
                color = colors.content
            )
        }

        // Dismiss button
        if (onDismiss != null) {
            val description = dismissContentDescription!!
            val trackedDismiss: () -> Unit = if (dismissAnalyticsId == null) onDismiss else {
                {
                    analytics.track(
                        event = "ds_alert_dismissed",
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
}

// ─── Internal ────────────────────────────────────────────────────────────────

private data class AlertColors(
    val background: Color,
    val content: Color,
)

@Composable
private fun resolveAlertColors(type: AlertType): AlertColors {
    val base = when (type) {
        AlertType.Danger  -> DsTheme.colors.danger
        AlertType.Warning -> DsTheme.colors.warning
        AlertType.Success -> DsTheme.colors.success
        AlertType.Info    -> DsTheme.colors.info
    }
    return AlertColors(
        background = base.copy(alpha = 0.15f),
        content    = base,
    )
}
