package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsSnackbarHost
import com.tekmoon.designsystem.components.DsSnackbarMessage
import com.tekmoon.designsystem.components.DsSnackbarType
import com.tekmoon.designsystem.components.rememberDsSnackbarController
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DsSnackbarTypesPreview() = DsPreviewScaffold(
    layout = { content -> content() }
) {
    // Render each type by pre-loading a controller with a message
    DsSnackbarType.entries.forEach { type ->
        val controller = rememberDsSnackbarController()
        controller.show(
            message = "${type.name} — something happened",
            type = type,
            durationMs = Long.MAX_VALUE  // keep visible in preview
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            DsSnackbarHost(
                controller = controller,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DsSnackbarWithActionPreview() = DsPreviewScaffold(
    layout = { content -> content() }
) {
    val controller = rememberDsSnackbarController()
    controller.show(
        DsSnackbarMessage(
            message = "File deleted",
            type = DsSnackbarType.Error,
            actionLabel = "Undo",
            onAction = {},
            durationMs = Long.MAX_VALUE
        )
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        DsSnackbarHost(
            controller = controller,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
