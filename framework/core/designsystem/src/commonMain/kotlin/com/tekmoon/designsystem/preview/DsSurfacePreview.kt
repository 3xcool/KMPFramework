package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.components.DsText
import com.tekmoon.designsystem.foundation.DsSurface
import com.tekmoon.designsystem.foundation.DsSurfaceRole
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The shadows will appear at runtime, but not (or barely) in Preview.
 */

@Composable
private fun PreviewSurface(
    title: String,
    role: DsSurfaceRole,
    modifier: Modifier = Modifier
) {
    DsSurface(
        role = role,
        onClick = {},
        onLongClick = {},
        modifier = modifier
            .height(80.dp)
    ) {
        Box(
            Modifier
                .padding(DsTheme.spacing.contentPadding),
            contentAlignment = Alignment.Center
        ) {
            DsText(
                text = title,
                style = DsTheme.typography.md.copy(
                    color = DsTheme.textColors.primary
                )
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 450
)
@Composable
fun DsSurface_StaticMatrix_Preview() {
    DsPreviewScaffold(
        layout = { content ->
            Row {
                content()
            }
        }
    ) {
        Column(
            Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DsText("hey")
            PreviewSurface("Flat", DsSurfaceRole.Flat)
            PreviewSurface("Card", DsSurfaceRole.Card)
            PreviewSurface("Floating", DsSurfaceRole.Floating)
            PreviewSurface("Modal", DsSurfaceRole.Modal)
            PreviewSurface("Pressed (forced)", DsSurfaceRole.Pressed)
        }
    }
}


@Preview(
    showBackground = true,
    widthDp = 600
)
@Composable
fun DsSurface_Interactive_Preview() {
    DsPreviewScaffold {
        Column(
            Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DsText(
                "Hover & Press these cards",
                style = DsTheme.typography.lg
            )

            repeat(3) { index ->
                DsSurface(
                    role = DsSurfaceRole.Card,
                    onClick = {},
                    modifier = Modifier
                        .height(72.dp)
                ) {
                    Box(
                        Modifier
                            .padding(DsTheme.spacing.cardPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        DsText("Interactive Card #$index")
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = false,
    widthDp = 620
)
@Composable
fun DsSurface_Performance_List_Preview() {
    DsPreviewScaffold {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = 100,
                key = { it } // important for stability
            ) { index ->
                DsSurface(
                    role = DsSurfaceRole.Card,
                    onClick = {},
                    modifier = Modifier
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DsText(
                            text = "Item #$index",
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DsSurface_RealWorld_Preview() {
    DsPreviewScaffold(
        layout = { content ->
            Column {
                content()
            }
        }
    ) {
        Column(
            Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DsSurface(
                role = DsSurfaceRole.Floating,
                onClick = {},
                shape = DsTheme.shapes.card,
            ) {
                Column(
                    Modifier.padding(DsTheme.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DsText("Account", style = DsTheme.typography.sm)
                    DsText("Balance: $1,250.00", style = DsTheme.typography.xl)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DsSurface(
                            role = DsSurfaceRole.Card,
                            shape = DsTheme.shapes.card,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                Modifier
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DsText("Send")
                            }
                        }

                        DsSurface(
                            role = DsSurfaceRole.Card,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                Modifier
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DsText("Request")
                            }
                        }
                    }
                }
            }
        }
    }
}
