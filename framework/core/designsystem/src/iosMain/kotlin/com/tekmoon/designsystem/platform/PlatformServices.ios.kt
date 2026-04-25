package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable
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
        override fun setStatusBar(color: androidx.compose.ui.graphics.Color, darkIcons: Boolean) {}
        override fun setNavigationBar(color: androidx.compose.ui.graphics.Color, darkIcons: Boolean) {}
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