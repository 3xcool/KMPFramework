package com.tekmoon.designsystem.platform

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberPlatformHaptics(): PlatformHaptics {
    val view = LocalView.current

    return remember(view) {
        object : PlatformHaptics {
            override fun click() {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }

            override fun longPress() {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
}