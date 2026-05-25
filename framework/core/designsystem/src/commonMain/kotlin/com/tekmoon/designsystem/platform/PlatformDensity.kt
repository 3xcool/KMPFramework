package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable

expect val platformDensityScale: Float

@Composable
expect fun platformOsFontScale(): Float
