package com.tekmoon.designsystem.foundation

import androidx.compose.ui.graphics.Paint
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

actual fun Paint.applyPlatformBlur(radiusPx: Float) {
    asFrameworkPaint().maskFilter =
        MaskFilter.makeBlur(FilterBlurMode.NORMAL, radiusPx)
}