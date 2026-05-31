package com.tekmoon.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.components.DsBottomSheet
import com.tekmoon.designsystem.components.DsBottomSheetState
import com.tekmoon.designsystem.components.DsText
import com.tekmoon.designsystem.components.rememberDsBottomSheetScope
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DsBottomSheetCollapsedPreview() = DsPreviewScaffold(
    layout = { content -> content() }
) {
    val scope = rememberDsBottomSheetScope(
        initialSheetState = DsBottomSheetState.HALF_EXPANDED,
        minSheetHeightRelative = 0f,
        maxSheetHeightRelative = 0.6f,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(DsTheme.colors.bg)
    ) {
        DsText(
            text = "Screen content behind the sheet",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
        DsBottomSheet(
            sheetScope = scope,
            showDragIndicator = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DsTheme.spacing.lg)
            ) {
                DsText("Sheet content", style = DsTheme.typography.lg)
                Spacer(Modifier.height(8.dp))
                DsText(
                    "Drag the handle up or down to change state.",
                    style = DsTheme.typography.sm,
                    color = DsTheme.colors.textMuted
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DsBottomSheetWithScrimPreview() = DsPreviewScaffold(
    layout = { content -> content() }
) {
    val scope = rememberDsBottomSheetScope(
        initialSheetState = DsBottomSheetState.HALF_EXPANDED,
        minSheetHeightRelative = 0f,
        maxSheetHeightRelative = 0.7f,
        hasHalfExpandedState = true,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(DsTheme.colors.bg)
    ) {
        DsBottomSheet(
            sheetScope = scope,
            hasScrim = true,
            showDragIndicator = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DsTheme.spacing.lg)
            ) {
                DsText("Sheet with scrim", style = DsTheme.typography.lg)
                Spacer(Modifier.height(8.dp))
                DsText(
                    "Tap the scrim or drag down to collapse.",
                    style = DsTheme.typography.sm,
                    color = DsTheme.colors.textMuted
                )
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
