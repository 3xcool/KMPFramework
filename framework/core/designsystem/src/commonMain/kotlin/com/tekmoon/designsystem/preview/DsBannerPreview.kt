package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsBanner
import com.tekmoon.designsystem.components.DsBannerAction
import com.tekmoon.designsystem.components.DsBannerType
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DsBannerTypesPreview() = DsPreviewScaffold {
    DsBannerType.entries.forEach { type ->
        DsBanner(
            title = "${type.name} banner",
            message = "Short description of what's going on.",
            type = type,
            primaryAction = DsBannerAction(label = "Action") {},
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DsBannerWithSecondaryActionPreview() = DsPreviewScaffold {
    DsBanner(
        title = "Update available",
        message = "Version 1.4 includes performance improvements and bug fixes.",
        type = DsBannerType.Info,
        primaryAction = DsBannerAction(label = "Update now") {},
        secondaryAction = DsBannerAction(label = "Later") {},
    )
    Spacer(Modifier.height(12.dp))
    DsBanner(
        title = "Delete account?",
        message = "This will permanently erase your data and cannot be undone.",
        type = DsBannerType.Danger,
        primaryAction = DsBannerAction(label = "Delete") {},
        secondaryAction = DsBannerAction(label = "Cancel") {},
    )
}

@Preview(showBackground = true)
@Composable
fun DsBannerDismissiblePreview() = DsPreviewScaffold {
    DsBanner(
        title = "Sync complete",
        message = "Your changes are saved across devices.",
        type = DsBannerType.Success,
        primaryAction = DsBannerAction(label = "View") {},
        onDismiss = {},
        dismissContentDescription = "Dismiss banner",
    )
    Spacer(Modifier.height(12.dp))
    DsBanner(
        title = "Storage almost full",
        message = "You've used 9.3 GB of 10 GB. Free up space to keep syncing.",
        type = DsBannerType.Warning,
        primaryAction = DsBannerAction(label = "Manage") {},
        secondaryAction = DsBannerAction(label = "Not now") {},
        onDismiss = {},
        dismissContentDescription = "Dismiss banner",
    )
}
