package com.tekmoon.designsystem.platform

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformAccessibility(): PlatformAccessibility {
    val context = LocalContext.current
    val manager = context.getSystemService(AccessibilityManager::class.java)

    return remember(manager) {
        object : PlatformAccessibility {
            override fun announce(message: String) {
                manager?.sendAccessibilityEvent(
                    android.view.accessibility.AccessibilityEvent.obtain().apply {
                        eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                        text.add(message)
                    }
                )
            }
        }
    }
}
