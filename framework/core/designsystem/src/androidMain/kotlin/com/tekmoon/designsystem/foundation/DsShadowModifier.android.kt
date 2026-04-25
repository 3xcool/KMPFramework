package com.tekmoon.designsystem.foundation

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint

actual fun Paint.applyPlatformBlur(radiusPx: Float) {
    asFrameworkPaint().maskFilter =
        BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
}