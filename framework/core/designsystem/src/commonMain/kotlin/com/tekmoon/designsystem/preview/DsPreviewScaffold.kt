package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.foundation.DsSurface
import com.tekmoon.designsystem.image.LocalDsImageLoader
import com.tekmoon.designsystem.tokens.AccentConfig
import com.tekmoon.designsystem.ui.LocalUiTextFallbackResolver
import com.tekmoon.designsystem.ui.PreviewUiTextFallbackResolver


/**
 * Usages:
 *
 * PreviewScaffold {
 *     // preview content
 * }
 *
 * PreviewScaffold(
 *     layout = { content ->
 *         Column {
 *             content()
 *         }
 *     }
 * ) {
 *     // preview content
 * }
 */
@Composable
fun DsPreviewScaffold(
    showLightMode: Boolean = true,
    showDarkMode: Boolean = true,
    accentConfig: AccentConfig? = null,
    padding: Dp = 16.dp,
    layout: @Composable (@Composable () -> Unit) -> Unit = { content ->
        Row {
            content()
        }
    },
    content: @Composable () -> Unit
) {

    val previewImageLoader = rememberPreviewImageLoader()

    //    val accentConfig = AccentConfig(
//        hue = 140f, // green
//        saturation = 0.6f
//    )
    val mAccentConfig = accentConfig ?: AccentConfig(
        hue = 280f,
        saturation = 0.55f
    )

    CompositionLocalProvider(
        LocalDsImageLoader provides previewImageLoader,
        LocalUiTextFallbackResolver provides PreviewUiTextFallbackResolver
    ) {
        layout {
            if (showLightMode) {
                DsTheme(
                    darkTheme = false,
                    accentConfig = mAccentConfig
                ) {
                    DsSurface {
                        Column(
                            modifier = Modifier.padding(padding)
                        ) {
                            content()
                        }
                    }
                }
            }

            if (showDarkMode) {
                DsTheme(
                    darkTheme = true,
                    accentConfig = mAccentConfig
                ) {
                    DsSurface {
                        Column(
                            modifier = Modifier.padding(padding)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}