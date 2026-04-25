package com.tekmoon.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.tokens.ElevationTokens

@Immutable
data class DsElevation(
    val levels: Map<Int, Dp>
) {
    fun dp(level: Int): Dp = levels[level] ?: 0.dp
}


val DefaultElevation = DsElevation(
    levels = mapOf(
        ElevationTokens.None to 0.dp,
        ElevationTokens.Card to 4.dp,
        ElevationTokens.Floating to 10.dp,
        ElevationTokens.Modal to 24.dp
    )
)