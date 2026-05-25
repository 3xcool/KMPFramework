package com.tekmoon.permissions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.components.DsButtonVariant
import com.tekmoon.designsystem.components.DsDialogWeb
import com.tekmoon.designsystem.image.DsImageDefaults
import com.tekmoon.designsystem.image.DsImageSource

/**
 * Rationale dialog shown before requesting a sensitive permission.
 *
 * Sits in `core/permissions` (Core Utils) rather than `core/designsystem` (Core / UI components)
 * because it bundles a UI primitive with a platform-permission behavior — UI components in Core
 * must stay agnostic.
 *
 * The actual rendering is delegated to [DsDialogWeb] so the look-and-feel stays aligned with the
 * rest of the design system.
 *
 * @param title         dialog title (typically "Allow camera access" etc.)
 * @param description   longer explanation of why the permission is needed
 * @param confirmText   label for the primary action; default = "Allow"
 * @param cancelText    label for the secondary action; default = "Cancel"
 * @param icon          optional decorative icon shown in the top-right corner
 * @param onConfirm     invoked when the user taps the confirm button (caller then triggers
 *                      `viewModel.onAction(PermissionsAction.RequestOne(...))` or similar)
 * @param onCancel      invoked when the user taps cancel or dismisses the dialog
 */
@Composable
fun PermissionDisclaimerDialog(
    title: String,
    description: String,
    confirmText: String = "Allow",
    cancelText: String? = "Cancel",
    icon: DsImageSource? = null,
    iconSize: Dp? = DsImageDefaults.defaultIconSize,
    iconTint: Color = DsTheme.colors.contentMuted,
    dialogModifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = onCancel,
    dismissOnOutsideClick: Boolean = true,
) {
    DsDialogWeb(
        title = title,
        description = description,
        confirmText = confirmText,
        onConfirm = onConfirm,
        icon = icon,
        iconSize = iconSize,
        iconTint = iconTint,
        cancelButtonVariant = DsButtonVariant.Outlined,
        confirmationButtonVariant = DsButtonVariant.Solid,
        dialogModifier = dialogModifier,
        cancelText = cancelText,
        onCancel = if (cancelText != null) onCancel else null,
        onDismissRequest = onDismissRequest,
        dismissOnOutsideClick = dismissOnOutsideClick,
    )
}
