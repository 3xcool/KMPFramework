package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable

// Desktop UIs usually need slightly more breathing room.
// Example Usage: val spacing = DsTheme.spacing.md * platformDensityScale
actual val platformDensityScale: Float = 1.15f

// Desktop has no OS-level dynamic font scale; return 1.0 so DsTheme typography is unchanged.
@Composable
actual fun platformOsFontScale(): Float = 1.0f
