package com.tekmoon.designsystem.platform

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformSystemUi(): PlatformSystemUi {
    val context = LocalContext.current
    val activity = context as? Activity

    return remember(activity) {
        object : PlatformSystemUi {
            override fun setStatusBar(
                color: Color,
                darkIcons: Boolean
            ) {
                activity?.window?.let { window ->
                    window.statusBarColor = color.toArgb()
                    WindowInsetsControllerCompat(window, window.decorView)
                        .isAppearanceLightStatusBars = darkIcons
                }
            }

            override fun setNavigationBar(
                color: Color,
                darkIcons: Boolean
            ) {
                activity?.window?.let { window ->
                    window.navigationBarColor = color.toArgb()
                    WindowInsetsControllerCompat(window, window.decorView)
                        .isAppearanceLightNavigationBars = darkIcons
                }
            }
        }
    }
}
