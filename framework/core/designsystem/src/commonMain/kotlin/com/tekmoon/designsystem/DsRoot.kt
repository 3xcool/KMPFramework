package com.tekmoon.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tekmoon.designsystem.foundation.DsSurface
import com.tekmoon.designsystem.platform.PlatformServicesProvider
import com.tekmoon.designsystem.platform.SystemUiEffect
import com.tekmoon.designsystem.tokens.AccentConfig

@Composable
fun DsRoot(
    darkTheme: Boolean,
    accentConfig: AccentConfig,
    content: @Composable () -> Unit
) {
    PlatformServicesProvider {
        DsTheme(
            darkTheme = darkTheme,
            accentConfig = accentConfig
        ) {
            DsSurface {
                // Optional but recommended
                SystemUiEffect(
                    statusBarColor = DsTheme.colors.bgDark,
                    darkIcons = !darkTheme
                )

                content()
            }
        }
    }
}

