package com.tekmoon.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 🔁 How colors should be used
 * Use case	             Color
 * Body text	         text
 * Secondary text	     textMuted
 * Icons / loaders	     content
 * Disabled icons	     contentMuted
 * Accent background	 primary
 * Content on accent	 onPrimary
 */

@Immutable
data class DsColors(
    // Surfaces
    val bgDark: Color,
    val bg: Color,
    val bgLight: Color,

    // Text
    val text: Color,
    val textMuted: Color,

    // Content (icons, loaders, indicators)
    val content: Color,
    val contentMuted: Color,

    // Accent
    val primary: Color,
    val primaryMuted: Color,
    val onPrimary: Color,

    // Alerts
    val danger: Color,
    val warning: Color,
    val success: Color,
    val info: Color
)
