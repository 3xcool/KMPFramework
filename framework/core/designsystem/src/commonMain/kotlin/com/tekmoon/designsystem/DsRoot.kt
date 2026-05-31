package com.tekmoon.designsystem

import androidx.compose.runtime.Composable
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

