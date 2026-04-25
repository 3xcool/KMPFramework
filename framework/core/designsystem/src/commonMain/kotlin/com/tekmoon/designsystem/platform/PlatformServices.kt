package com.tekmoon.designsystem.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp


interface PlatformHaptics {
    fun click()
    fun longPress()
}

interface PlatformSystemUi {
    fun setStatusBar(color: Color, darkIcons: Boolean)
    fun setNavigationBar(color: Color, darkIcons: Boolean)
}

interface PlatformInsets {
    val statusBarTop: Dp
    val navigationBarBottom: Dp
}

interface PlatformAccessibility {
    fun announce(message: String)
}


@Composable
expect fun rememberPlatformHaptics(): PlatformHaptics

@Composable
expect fun rememberPlatformSystemUi(): PlatformSystemUi

@Composable
expect fun rememberPlatformInsets(): PlatformInsets

@Composable
expect fun rememberPlatformAccessibility(): PlatformAccessibility


// Extension
@Composable
fun Modifier.hapticClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptics = rememberPlatformHaptics()

    return this.clickable(
        enabled = enabled
    ) {
        haptics.click()
        onClick()
    }
}


@Composable
fun Modifier.hapticLongClick(
    enabled: Boolean = true,
    onLongClick: () -> Unit
): Modifier {
    val haptics = rememberPlatformHaptics()

    return this.clickable(
        enabled = enabled
    ) {
        haptics.longPress()
        onLongClick()
    }
}

@Composable
fun Modifier.hapticCombinedClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier {
    val haptics = rememberPlatformHaptics()

    return this.combinedClickable(
        enabled = enabled,
        onLongClick = {
            haptics.longPress()
            onLongClick()
        },
        onClick =  {
            haptics.click()
            onClick()
        }
    )
}