package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.AlertType
import com.tekmoon.designsystem.components.DsAlert
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DsAlertTypesPreview() = DsPreviewScaffold {
    AlertType.entries.forEach { type ->
        DsAlert(
            message = "${type.name} alert message",
            type = type
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DsAlertWithTitlePreview() = DsPreviewScaffold {
    AlertType.entries.forEach { type ->
        DsAlert(
            title = "${type.name} title",
            message = "This is a longer description that explains the ${type.name.lowercase()} state in more detail.",
            type = type
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DsAlertDismissiblePreview() = DsPreviewScaffold {
    DsAlert(
        title = "Something went wrong",
        message = "Please try again later.",
        type = AlertType.Danger,
        onDismiss = {},
        dismissContentDescription = "Dismiss alert",
    )
    Spacer(Modifier.height(8.dp))
    DsAlert(
        message = "Your changes have been saved.",
        type = AlertType.Success,
        onDismiss = {},
        dismissContentDescription = "Dismiss alert",
    )
}
