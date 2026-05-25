package com.tekmoon.designsystem.preview


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsButton
import com.tekmoon.designsystem.components.DsButtonIconPosition
import com.tekmoon.designsystem.components.DsButtonIntent
import com.tekmoon.designsystem.components.DsButtonLoadingMode
import com.tekmoon.designsystem.components.DsButtonSize
import com.tekmoon.designsystem.components.DsButtonVariant
import com.tekmoon.designsystem.image.DsImageSource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.tekmoon.designsystem.generated.resources.Res
import com.tekmoon.designsystem.generated.resources.ic_outline_check_24


@Preview(showBackground = true)
@Composable
fun DsButtonVariantPreview() = DsPreviewScaffold {
    DsButton(
        text = "Primary Solid",
        intent = DsButtonIntent.Primary,
        variant = DsButtonVariant.Solid,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Secondary Solid",
        intent = DsButtonIntent.Secondary,
        variant = DsButtonVariant.Solid,
        onClick = {}
    )

    Spacer(Modifier.height(24.dp))

    DsButton(
        text = "Primary Outlined",
        intent = DsButtonIntent.Primary,
        variant = DsButtonVariant.Outlined,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Primary Text",
        intent = DsButtonIntent.Primary,
        variant = DsButtonVariant.Text,
        onClick = {}
    )

    Spacer(Modifier.height(24.dp))

    DsButton(
        text = "Destructive Solid",
        intent = DsButtonIntent.Destructive,
        variant = DsButtonVariant.Solid,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Destructive Outlined",
        intent = DsButtonIntent.Destructive,
        variant = DsButtonVariant.Outlined,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Destructive Text",
        intent = DsButtonIntent.Destructive,
        variant = DsButtonVariant.Text,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DsButtonStatePreview() = DsPreviewScaffold {

    DsButton(
        text = "Enabled",
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Disabled",
        enabled = false,
        onClick = {}
    )

    Spacer(Modifier.height(24.dp))

    DsButton(
        text = "Loading (Replace)",
        loading = true,
        loadingMode = DsButtonLoadingMode.ReplaceContent,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Loading (Keep Text)",
        loading = true,
        loadingMode = DsButtonLoadingMode.KeepText,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DsButtonSizePreview() = DsPreviewScaffold {

    DsButton(
        text = "Small",
        size = DsButtonSize.Small,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Medium",
        size = DsButtonSize.Medium,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Large",
        size = DsButtonSize.Large,
        onClick = {}
    )

    Spacer(Modifier.height(24.dp))

    DsButton(
        text = "Custom",
        size = DsButtonSize.Custom(
            horizontalPadding = 20.dp,
            verticalPadding = 6.dp,
            minHeight = 36.dp,
            iconSize = 12.dp
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DsButtonIconPreview() = DsPreviewScaffold {

    DsButton(
        text = "Icon Start",
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        iconPosition = DsButtonIconPosition.Start,
        onClick = {}
    )

    Spacer(Modifier.height(12.dp))

    DsButton(
        text = "Icon End",
        icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
        iconPosition = DsButtonIconPosition.End,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DsButtonMatrixPreview() = DsPreviewScaffold {

    DsButtonVariant.entries.forEach { variant ->
        DsButtonIntent.entries.forEach { intent ->
            DsButton(
                text = "$intent $variant",
                variant = variant,
                intent = intent,
                size = DsButtonSize.Medium,
                onClick = {}
            )

            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}