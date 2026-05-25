package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable

actual val platformDensityScale: Float = 1.0f

// Compose `.sp` on Android already incorporates Configuration.fontScale via LocalDensity,
// so multiplying typography by an extra factor here would double-scale. Return 1.0f and let
// the platform's own font-scale plumbing do the work.
@Composable
actual fun platformOsFontScale(): Float = 1.0f
