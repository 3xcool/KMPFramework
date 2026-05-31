package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsToastHost
import com.tekmoon.designsystem.components.DsToastMessage
import com.tekmoon.designsystem.components.DsToastType
import com.tekmoon.designsystem.components.rememberDsToastController
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DsToastTypesPreview() = DsPreviewScaffold(
    layout = { content -> content() }
) {
    DsToastType.entries.forEach { type ->
        val controller = rememberDsToastController()
        controller.show(
            DsToastMessage(
                message = "${type.name} — saved",
                type = type,
                durationMs = Long.MAX_VALUE,  // keep visible in preview
            )
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            DsToastHost(
                controller = controller,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DsToastLongMessagePreview() = DsPreviewScaffold(
    layout = { content -> content() }
) {
    val controller = rememberDsToastController()
    controller.show(
        DsToastMessage(
            message = "Connection restored. Pending changes will sync in the background.",
            type = DsToastType.Success,
            durationMs = Long.MAX_VALUE,
        )
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        DsToastHost(
            controller = controller,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
