package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsButtonIntent
import com.tekmoon.designsystem.components.DsButtonVariant
import com.tekmoon.designsystem.components.DsIconButton
import com.tekmoon.designsystem.components.DsIconButtonSize
import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.generated.resources.Res
import com.tekmoon.designsystem.generated.resources.ic_outline_check_24
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DsIconButtonSizesPreview() = DsPreviewScaffold {
    DsIconButton(
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        contentDescription = "Small",
        size = DsIconButtonSize.Small,
        onClick = {}
    )
    Spacer(Modifier.height(8.dp))
    DsIconButton(
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        contentDescription = "Medium",
        size = DsIconButtonSize.Medium,
        onClick = {}
    )
    Spacer(Modifier.height(8.dp))
    DsIconButton(
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        contentDescription = "Large",
        size = DsIconButtonSize.Large,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DsIconButtonMatrixPreview() = DsPreviewScaffold {
    DsButtonVariant.entries.forEach { variant ->
        DsButtonIntent.entries.forEach { intent ->
            DsIconButton(
                icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
                contentDescription = "$intent $variant",
                intent = intent,
                variant = variant,
                onClick = {}
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DsIconButtonStatesPreview() = DsPreviewScaffold {
    DsIconButton(
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        contentDescription = "Enabled",
        variant = DsButtonVariant.Solid,
        onClick = {}
    )
    Spacer(Modifier.height(8.dp))
    DsIconButton(
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        contentDescription = "Disabled",
        variant = DsButtonVariant.Solid,
        enabled = false,
        onClick = {}
    )
    Spacer(Modifier.height(8.dp))
    DsIconButton(
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        contentDescription = "Destructive",
        intent = DsButtonIntent.Destructive,
        variant = DsButtonVariant.Outlined,
        onClick = {}
    )
}
