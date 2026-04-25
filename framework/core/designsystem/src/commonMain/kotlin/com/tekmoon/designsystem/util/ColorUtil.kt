package com.tekmoon.designsystem.util

import androidx.compose.ui.graphics.Color

fun hslNeutral(lightness: Float): Color =
    Color.hsl(
        hue = 0f,
        saturation = 0f,
        lightness = lightness
    )

fun hslAccent(
    hue: Float,
    saturation: Float,
    lightness: Float
): Color =
    Color.hsl(
        hue = hue,
        saturation = saturation,
        lightness = lightness
    )
