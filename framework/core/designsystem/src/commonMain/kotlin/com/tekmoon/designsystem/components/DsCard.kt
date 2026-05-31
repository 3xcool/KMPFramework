package com.tekmoon.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.foundation.DsSurface
import com.tekmoon.designsystem.foundation.DsSurfaceRole

@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    DsSurface(
        modifier = modifier,
        role = DsSurfaceRole.Card,
        shape = DsTheme.shapes.card
    ) {
        Box(Modifier.padding(DsTheme.spacing.cardPadding)) {
            content()
        }
    }
}
