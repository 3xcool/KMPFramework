package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun PlatformServicesProvider(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPlatformHaptics provides rememberPlatformHaptics(),
        LocalPlatformSystemUi provides rememberPlatformSystemUi(),
        LocalPlatformInsets provides rememberPlatformInsets(),
        LocalPlatformAccessibility provides rememberPlatformAccessibility(),
        content = content
    )
}