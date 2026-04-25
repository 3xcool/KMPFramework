package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext

typealias PlatformContext = coil3.PlatformContext

@Composable
fun currentPlatformContext(): PlatformContext = LocalPlatformContext.current

