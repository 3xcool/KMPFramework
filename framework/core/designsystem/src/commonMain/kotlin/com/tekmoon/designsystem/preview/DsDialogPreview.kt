package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsDialogMaterial
import com.tekmoon.designsystem.components.DsDialogWeb
import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.image.LocalDsImageLoader
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.tekmoon.designsystem.generated.resources.Res
import com.tekmoon.designsystem.generated.resources.dog_man_image
import com.tekmoon.designsystem.generated.resources.ic_outline_check_24

@Preview(
    group = "DsDialogPreview",
    showBackground = true,
    heightDp = 1200
)
@Composable
private fun DsDialogPreviewSimple() {
    val previewImageLoader = rememberPreviewImageLoader()

    DsPreviewScaffold(
        layout = {
            Column {
                it()
            }
        }
    ) {
        CompositionLocalProvider(
            LocalDsImageLoader provides previewImageLoader
        ) {

            DsDialogWeb(
                rootModifier = Modifier,
                title = "Delete item",
                description = "This action cannot be undone.",
                icon = DsImageSource.DrawableImage(Res.drawable.dog_man_image),
                iconSize = 15.dp,
                confirmText = "Delete",
                onConfirm = {},
                onDismissRequest = { }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DsDialogMaterial(
                rootModifier = Modifier,
                title = "Delete item",
                description = "This action cannot be undone.",
                icon = DsImageSource.Remote(""),
                iconSize = 48.dp,
                confirmText = "Delete",
                onConfirm = {},
                onDismissRequest = { }
            )
        }
    }
}

@Preview(
    group = "DsDialogPreview",
    showBackground = true,
    heightDp = 1200
)
@Composable
private fun DsDialogPreviewTwoActions() {
    DsPreviewScaffold(
        layout = {
            Column {
                it()
            }
        }
    ) {
        DsDialogWeb(
            rootModifier = Modifier,
            title = "Sign out",
            description = "You will need to log in again.",
            icon = DsImageSource.DrawableIcon(Res.drawable.ic_outline_check_24),
            iconSize = 40.dp,
            confirmText = "Sign out",
            cancelText = "Cancel",
            onConfirm = {},
            onCancel = {},
            onDismissRequest = {}
        )
        Spacer(modifier = Modifier.height(16.dp))
        DsDialogMaterial(
            rootModifier = Modifier,
            title = "Sign out",
            description = "You will need to log in again.",
            icon = DsImageSource.DrawableIcon(Res.drawable.ic_outline_check_24),
//            iconSize = 120.dp,
            confirmText = "Sign out",
            cancelText = "Cancel",
            onConfirm = {},
            onCancel = {},
            onDismissRequest = {}
        )
    }
}

@Preview(
    group = "DsDialogPreview",
    showBackground = true,
    heightDp = 1200
)
@Composable
private fun DsDialogPreviewBlocked() {
    DsPreviewScaffold(
        layout = {
            Column {
                it()
            }
        }
    ) {
        DsDialogWeb(
            rootModifier = Modifier,
            title = "Critical update",
            description = "You must update the app to continue.",
            confirmText = "Update",
            dismissOnOutsideClick = false,
            onConfirm = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        DsDialogMaterial(
            rootModifier = Modifier,
            title = "Critical update",
            description = "You must update the app to continue.",
            confirmText = "Update",
            dismissOnOutsideClick = false,
            onConfirm = {}
        )
    }
}