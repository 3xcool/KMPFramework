package com.tekmoon.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.analytics.LocalAnalytics
import com.tekmoon.designsystem.foundation.DsSurface
import com.tekmoon.designsystem.foundation.DsSurfaceRole
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageDefaults
import com.tekmoon.designsystem.image.DsImageSource


@Composable
fun DsDialogWeb(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,

    icon: DsImageSource? = null,
    iconSize: Dp? = DsImageDefaults.defaultIconSize,
    iconTint: Color = DsTheme.colors.contentMuted,

    cancelButtonVariant: DsButtonVariant= DsButtonVariant.Outlined,
    confirmationButtonVariant: DsButtonVariant = DsButtonVariant.Solid,

    rootModifier: Modifier = Modifier.fillMaxSize(),
    dialogModifier: Modifier = Modifier,

    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,

    onDismissRequest: (() -> Unit)? = null,
    dismissOnOutsideClick: Boolean = true,

    scrimColor: Color = DsTheme.colors.bgDark.copy(alpha = 0.6f),

    /** Stable identifier emitted with `"ds_dialog_confirmed"` when the confirm button fires. */
    confirmAnalyticsId: String? = null,
    /** Stable identifier emitted with `"ds_dialog_cancelled"` when the cancel button fires.
     *  Ignored if [onCancel] is `null`. */
    cancelAnalyticsId: String? = null,
    /** Stable identifier emitted with `"ds_dialog_dismissed"` when the scrim is tapped
     *  ([dismissOnOutsideClick] = true) or [onDismissRequest] otherwise fires. */
    dismissAnalyticsId: String? = null,
    /** Extra params merged into every event this dialog emits (alongside `id` + `title`). */
    analyticsParams: Map<String, Any?> = emptyMap(),
) {
    val analytics = LocalAnalytics.current
    val trackedConfirm: () -> Unit = if (confirmAnalyticsId == null) onConfirm else {
        {
            analytics.track(
                event = "ds_dialog_confirmed",
                params = mapOf("id" to confirmAnalyticsId, "title" to title) + analyticsParams,
            )
            onConfirm()
        }
    }
    val trackedCancel: (() -> Unit)? = onCancel?.let { raw ->
        if (cancelAnalyticsId == null) raw else {
            {
                analytics.track(
                    event = "ds_dialog_cancelled",
                    params = mapOf("id" to cancelAnalyticsId, "title" to title) + analyticsParams,
                )
                raw()
            }
        }
    }
    val trackedDismiss: (() -> Unit)? = onDismissRequest?.let { raw ->
        if (dismissAnalyticsId == null) raw else {
            {
                analytics.track(
                    event = "ds_dialog_dismissed",
                    params = mapOf("id" to dismissAnalyticsId, "title" to title) + analyticsParams,
                )
                raw()
            }
        }
    }
    // Scrim + dialog container
    Box(
        modifier = rootModifier
            .background(scrimColor)
            .then(
                if (dismissOnOutsideClick && trackedDismiss != null)
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { trackedDismiss() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Prevent outside clicks from propagating
        Box(
            Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
        ) {
            DsSurface(
                role = DsSurfaceRole.Modal,
                shape = DsTheme.shapes.dialog,
                modifier = dialogModifier
                    .widthIn(min = 280.dp, max = 360.dp)
            ) {
                // Icon (top-right)
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(DsTheme.spacing.sm)
                    ) {
                        DsImage(
                            source = icon,
                            contentDescription = null,
                            tint = iconTint,
                            iconSize = iconSize,
                            imageSize = iconSize
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(DsTheme.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    DsText(
                        text = title,
                        style = DsTheme.typography.lg
                    )

                    // Description
                    DsText(
                        text = description,
                        style = DsTheme.typography.md,
                        color = DsTheme.colors.textMuted
                    )

                    Spacer(Modifier.height(8.dp))

                    // Actions
                    ButtonActionsAsWeb(
                        confirmText = confirmText,
                        onConfirm = trackedConfirm,
                        cancelButtonVariant = cancelButtonVariant,
                        confirmationButtonVariant = confirmationButtonVariant,
                        cancelText = cancelText,
                        onCancel = trackedCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonActionsAsWeb(
    confirmText: String,
    onConfirm: () -> Unit,
    cancelButtonVariant: DsButtonVariant= DsButtonVariant.Outlined,
    confirmationButtonVariant: DsButtonVariant = DsButtonVariant.Solid,
    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cancelText != null && onCancel != null) {
            DsButton(
                text = cancelText,
                variant = cancelButtonVariant,
                onClick = onCancel
            )

            Spacer(Modifier.width(8.dp))
        }

        DsButton(
            text = confirmText,
            variant = confirmationButtonVariant,
            onClick = onConfirm
        )
    }
}

@Composable
fun DsDialogMaterial(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,

    icon: DsImageSource? = null,
    iconSize: Dp? = DsImageDefaults.DsIconSize.Dialog.size,
    iconTint: Color = DsTheme.colors.contentMuted,

    cancelButtonVariant: DsButtonVariant= DsButtonVariant.Outlined,
    confirmationButtonVariant: DsButtonVariant = DsButtonVariant.Solid,

    rootModifier: Modifier = Modifier.fillMaxSize(),
    dialogModifier: Modifier = Modifier,

    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,

    onDismissRequest: (() -> Unit)? = null,
    dismissOnOutsideClick: Boolean = true,

    scrimColor: Color = DsTheme.colors.bgDark.copy(alpha = 0.6f),

    /** Stable identifier emitted with `"ds_dialog_confirmed"` when the confirm button fires. */
    confirmAnalyticsId: String? = null,
    /** Stable identifier emitted with `"ds_dialog_cancelled"` when the cancel button fires.
     *  Ignored if [onCancel] is `null`. */
    cancelAnalyticsId: String? = null,
    /** Stable identifier emitted with `"ds_dialog_dismissed"` when the scrim is tapped
     *  ([dismissOnOutsideClick] = true) or [onDismissRequest] otherwise fires. */
    dismissAnalyticsId: String? = null,
    /** Extra params merged into every event this dialog emits (alongside `id` + `title`). */
    analyticsParams: Map<String, Any?> = emptyMap(),
) {
    val analytics = LocalAnalytics.current
    val trackedConfirm: () -> Unit = if (confirmAnalyticsId == null) onConfirm else {
        {
            analytics.track(
                event = "ds_dialog_confirmed",
                params = mapOf("id" to confirmAnalyticsId, "title" to title) + analyticsParams,
            )
            onConfirm()
        }
    }
    val trackedCancel: (() -> Unit)? = onCancel?.let { raw ->
        if (cancelAnalyticsId == null) raw else {
            {
                analytics.track(
                    event = "ds_dialog_cancelled",
                    params = mapOf("id" to cancelAnalyticsId, "title" to title) + analyticsParams,
                )
                raw()
            }
        }
    }
    val trackedDismiss: (() -> Unit)? = onDismissRequest?.let { raw ->
        if (dismissAnalyticsId == null) raw else {
            {
                analytics.track(
                    event = "ds_dialog_dismissed",
                    params = mapOf("id" to dismissAnalyticsId, "title" to title) + analyticsParams,
                )
                raw()
            }
        }
    }
    // Scrim + dialog container
    Box(
        modifier = rootModifier
            .background(scrimColor)
            .then(
                if (dismissOnOutsideClick && trackedDismiss != null)
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { trackedDismiss() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Prevent outside clicks from propagating
        Box(
            Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
        ) {
            DsSurface(
                role = DsSurfaceRole.Modal,
                shape = DsTheme.shapes.dialog,
                modifier = dialogModifier
                    .widthIn(min = 280.dp, max = 360.dp)
            ) {
                Column(
                    modifier = Modifier.padding(DsTheme.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    icon?.let{
                        DsImage(
                            modifier = Modifier,
                            source = icon,
                            contentDescription = null,
                            tint = iconTint,
                            iconSize = iconSize,
                            imageSize = iconSize
                        )
                    }

                    // Title
                    DsText(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = DsTheme.typography.lg
                    )

                    // Description
                    DsText(
                        text = description,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = DsTheme.typography.md,
                        color = DsTheme.colors.textMuted
                    )

                    Spacer(Modifier.height(8.dp))

                    // Actions
                    ButtonActionsAsMaterial(
                        confirmText = confirmText,
                        onConfirm = trackedConfirm,
                        cancelButtonVariant = cancelButtonVariant,
                        confirmationButtonVariant = confirmationButtonVariant,
                        cancelText = cancelText,
                        onCancel = trackedCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonActionsAsMaterial(
    confirmText: String,
    onConfirm: () -> Unit,
    cancelButtonVariant: DsButtonVariant= DsButtonVariant.Outlined,
    confirmationButtonVariant: DsButtonVariant = DsButtonVariant.Solid,
    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DsTheme.spacing.sm)
    ) {
        if (cancelText != null && onCancel != null) {
            DsButton(
                text = cancelText,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                variant = cancelButtonVariant,
            )
        }

        DsButton(
            text = confirmText,
            variant = confirmationButtonVariant,
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        )
    }
}