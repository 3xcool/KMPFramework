package com.tekmoon.designsystem.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
actual fun rememberPlatformInsets(): PlatformInsets {
    val density = LocalDensity.current
    val insets = WindowInsets.systemBars

    return object : PlatformInsets {
        override val statusBarTop =
            with(density) { insets.getTop(density).toDp() }

        override val navigationBarBottom =
            with(density) { insets.getBottom(density).toDp() }
    }
}