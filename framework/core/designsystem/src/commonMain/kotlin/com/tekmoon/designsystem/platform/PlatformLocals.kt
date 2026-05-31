@file:Suppress("EmptyFunctionBlock")

package com.tekmoon.designsystem.platform

// Empty function bodies are intentional: these CompositionLocal defaults
// are no-op implementations used when no real platform service is provided.

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalPlatformHaptics = staticCompositionLocalOf<PlatformHaptics> {
    object : PlatformHaptics {
        override fun click() {}
        override fun longPress() {}
    }
}

val LocalPlatformSystemUi = staticCompositionLocalOf<PlatformSystemUi> {
    object : PlatformSystemUi {
        override fun setStatusBar(color: androidx.compose.ui.graphics.Color, darkIcons: Boolean) {}
        override fun setNavigationBar(color: androidx.compose.ui.graphics.Color, darkIcons: Boolean) {}
    }
}

val LocalPlatformInsets = staticCompositionLocalOf<PlatformInsets> {
    object : PlatformInsets {
        override val statusBarTop = 0.dp
        override val navigationBarBottom = 0.dp
    }
}

val LocalPlatformAccessibility = staticCompositionLocalOf<PlatformAccessibility> {
    object : PlatformAccessibility {
        override fun announce(message: String) {}
    }
}