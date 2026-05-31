package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.components.DsText
import com.tekmoon.designsystem.components.DsTextBody
import com.tekmoon.designsystem.components.DsTextCaption
import com.tekmoon.designsystem.components.DsTextDisplay
import com.tekmoon.designsystem.components.DsTextLabel
import com.tekmoon.designsystem.components.DsTextSubTitle
import com.tekmoon.designsystem.components.DsTextTitle
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val SAMPLE_TEXT =
    "The quick brown fox jumps over the lazy dog"


@Preview(
    showBackground = true,
    heightDp = 1000,
    widthDp = 320
)
@Composable
fun DsText_Semantic_Preview() {
    val spacing = 16.dp
    DsPreviewScaffold(
        layout = { content ->
            Column {
                content()
            }
        }
    ) {
        DsTextDisplay(
            text = "DsTextDisplay · 24.sp · Bold · primary · $SAMPLE_TEXT"
        )
        Spacer(modifier = Modifier.height(spacing))
        DsTextTitle(
            text = "DsTextTitle · 20.sp · Bold · primary · $SAMPLE_TEXT"
        )
        Spacer(modifier = Modifier.height(spacing))
        DsTextSubTitle(
            text = "DsTextSubTitle · 18.sp · Regular · secondary · $SAMPLE_TEXT"
        )
        Spacer(modifier = Modifier.height(spacing))
        DsTextBody(
            text = "DsTextBody · 16.sp · Regular · primary · $SAMPLE_TEXT"
        )
        Spacer(modifier = Modifier.height(spacing))
        DsTextLabel(
            text = "DsTextLabel · 14.sp · Regular · secondary · $SAMPLE_TEXT"
        )
        Spacer(modifier = Modifier.height(spacing))
        DsTextCaption(
            text = "DsTextCaption · 12.sp · Regular · tertiary · $SAMPLE_TEXT"
        )
    }
}

@Preview(
    showBackground = true,
    heightDp = 1500,
    widthDp = 1000
)
@Composable
fun DsText_AllTypography_AllTextColors_Preview() {
    DsPreviewScaffold(
        layout = { content ->
            Row {
                content()
            }
        }
    ) {
        val typographyVariants = listOf(
            "12.sp · Regular" to DsTheme.typography.xs,
            "14.sp · Regular" to DsTheme.typography.sm,
            "16.sp · Regular" to DsTheme.typography.base,
            "18.sp · Regular" to DsTheme.typography.md,
            "20.sp · Bold" to DsTheme.typography.lg,
            "24.sp · Bold" to DsTheme.typography.xl,
        )

        val textColorVariants = listOf(
            "primary · 100%" to DsTheme.textColors.primary,
            "secondary · 70%" to DsTheme.textColors.secondary,
            "tertiary · 50%" to DsTheme.textColors.tertiary,
            "disabled · 30%" to DsTheme.textColors.disabled,
        )

        for ((typoLabel, textStyle) in typographyVariants) {
            for ((colorLabel, color) in textColorVariants) {
                DsText(
                    text = "$typoLabel · $colorLabel\n$SAMPLE_TEXT",
//                    text = "$typoLabel · $colorLabel",
                    style = textStyle,
                    color = color
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
