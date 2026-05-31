package com.tekmoon.designsystem.platform


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SystemUiEffect(
    statusBarColor: androidx.compose.ui.graphics.Color,
    darkIcons: Boolean
) {
    val systemUi = LocalPlatformSystemUi.current

    LaunchedEffect(statusBarColor, darkIcons) {
        systemUi.setStatusBar(statusBarColor, darkIcons)
    }
}