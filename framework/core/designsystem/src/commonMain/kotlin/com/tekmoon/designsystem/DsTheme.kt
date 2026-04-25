package com.tekmoon.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import coil3.ImageLoader
import com.tekmoon.designsystem.foundation.*
import com.tekmoon.designsystem.image.DsMotionLevel
import com.tekmoon.designsystem.image.LocalDsImageLoader
import com.tekmoon.designsystem.image.LocalDsMotion
import com.tekmoon.designsystem.platform.currentPlatformContext
import com.tekmoon.designsystem.tokens.*
import com.tekmoon.designsystem.foundation.DarkTextColors
import com.tekmoon.designsystem.foundation.DsTypography
import com.tekmoon.designsystem.foundation.LightTextColors
import com.tekmoon.designsystem.foundation.LocalDsTextColors
import com.tekmoon.designsystem.foundation.LocalDsTypography
import com.tekmoon.designsystem.util.*

/**
 * Tokens        →  raw numbers (design source of truth)
 * Foundation    →  normalized, typed, semantic-ready
 * Theme         →  picks defaults
 * Components    →  never guess numbers
 */
@Composable
fun DsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentConfig: AccentConfig = AccentConfig(
        hue = 220f, // blue
        saturation = 0.6f
    ),
    content: @Composable () -> Unit
) {
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors

    val context = currentPlatformContext()
    val imageLoader = remember {
        ImageLoader.Builder(context).build()
    }

    val colors = DsColors(
        bgDark = hslNeutral(semantic.bgDark),
        bg = hslNeutral(semantic.bg),
        bgLight = hslNeutral(semantic.bgLight),

        text = hslNeutral(semantic.text),
        textMuted = hslNeutral(semantic.textMuted),

        content = hslNeutral(semantic.text),
        contentMuted = hslNeutral(semantic.textMuted),

        primary = hslAccent(
            accentConfig.hue,
            accentConfig.saturation,
            lightness = if (darkTheme) 0.55f else 0.50f
        ),
        primaryMuted = hslAccent(
            accentConfig.hue,
            accentConfig.saturation * 0.6f,
            lightness = if (darkTheme) 0.45f else 0.60f
        ),
        onPrimary = if (darkTheme) {
            hslNeutral(0.95f) // near white
        } else {
            hslNeutral(0.05f)  // near black
        },

        danger = hslAccent(AlertHues.Danger, 0.65f, 0.55f),
        warning = hslAccent(AlertHues.Warning, 0.65f, 0.55f),
        success = hslAccent(AlertHues.Success, 0.55f, 0.50f),
        info = hslAccent(AlertHues.Info, 0.60f, 0.55f)
    )

    // Typography
    val typography = DsTypography

    val textColors = if (darkTheme) {
        DarkTextColors
    } else {
        LightTextColors
    }

    val surfaceColors = if (darkTheme) {
        DsSurfaceColors(
            flat = colors.bgDark,
            raised = colors.bg,
            floating = colors.bgLight,
            modal = colors.bgLight
        )
    } else {
        DsSurfaceColors(
            flat = colors.bg,
            raised = colors.bgLight,
            floating = colors.bgLight,
            modal = colors.bgLight
        )
    }

    CompositionLocalProvider(
        LocalDsColors provides colors,
        LocalDsSpacing provides DefaultSpacing,
        LocalDsShapes provides DsShapesDefault,
        LocalDsElevation provides DefaultElevation,
        LocalDsShadows provides DefaultShadows,
        LocalDsMotion provides DsMotionLevel.Full,
        LocalDsImageLoader provides imageLoader,
        LocalDsTypography provides typography,
        LocalDsTextColors provides textColors,
        LocalDsSurfaceColors provides surfaceColors,
        content = content
    )
}

private val LocalDsColors = staticCompositionLocalOf<DsColors> {
    error("DsColors not provided")
}

private val LocalDsShadows = staticCompositionLocalOf<DsShadows> {
    error("DsShadows not provided")
}

private val LocalDsSpacing = staticCompositionLocalOf<DsSpacing> { error("No spacing") }
private val LocalDsShapes = staticCompositionLocalOf<DsShapes> { error("No shapes") }
private val LocalDsElevation = staticCompositionLocalOf<DsElevation> { error("No elevation") }


object DsTheme {
    val colors @Composable get() = LocalDsColors.current
    val textColors @Composable get() = LocalDsTextColors.current
    val typography @Composable get() = LocalDsTypography.current
    val spacing @Composable get() = LocalDsSpacing.current
    val shapes @Composable get() = LocalDsShapes.current
    val elevation @Composable get() = LocalDsElevation.current
    val shadows @Composable get() = LocalDsShadows.current
    val surfaceColors @Composable get() = LocalDsSurfaceColors.current
}