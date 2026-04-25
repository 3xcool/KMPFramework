package com.tekmoon.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tekmoon.designsystem.DsTheme

@Composable
fun DsAlert(
    message: String,
    type: AlertType
) {
    val color = when (type) {
        AlertType.Danger -> DsTheme.colors.danger
        AlertType.Warning -> DsTheme.colors.warning
        AlertType.Success -> DsTheme.colors.success
        AlertType.Info -> DsTheme.colors.info
    }

    Box(
        Modifier
            .clip(DsTheme.shapes.md)
            .background(color)
            .padding(DsTheme.spacing.md)
    ) {
        DsText(message, color = DsTheme.colors.text)
    }
}

enum class AlertType { Danger, Warning, Success, Info }
