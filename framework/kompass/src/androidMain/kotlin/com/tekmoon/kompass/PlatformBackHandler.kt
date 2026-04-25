package com.tekmoon.kompass

/**
 * Created by Andre Filgueiras on 14/09/24.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.tekmoon.kompass.util.BackPressedChannel
import kotlinx.coroutines.channels.Channel

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    backPressedChannel: BackPressedChannel?, // for desktop
    onBack: () -> Unit
) {
    BackHandler(enabled, onBack)
}