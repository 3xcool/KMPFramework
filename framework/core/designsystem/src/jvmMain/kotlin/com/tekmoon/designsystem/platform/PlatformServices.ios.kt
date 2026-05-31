@file:Suppress("EmptyFunctionBlock")

package com.tekmoon.designsystem.platform

// Empty function bodies are intentional: these are JVM/Desktop placeholder
// implementations. Haptics and system-chrome have no meaningful desktop
// equivalent for now, so no-op is the explicit behavior.

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberPlatformHaptics(): PlatformHaptics =
    object : PlatformHaptics {
        override fun click() {}
        override fun longPress() {}
    }

@Composable
actual fun rememberPlatformSystemUi(): PlatformSystemUi =
    object : PlatformSystemUi {
        override fun setStatusBar(color: Color, darkIcons: Boolean) {}
        override fun setNavigationBar(color: Color, darkIcons: Boolean) {}
    }

@Composable
actual fun rememberPlatformInsets(): PlatformInsets =
    object : PlatformInsets {
        override val statusBarTop = 0.dp
        override val navigationBarBottom = 0.dp
    }

@Composable
actual fun rememberPlatformAccessibility(): PlatformAccessibility =
    object : PlatformAccessibility {
        override fun announce(message: String) {}
    }