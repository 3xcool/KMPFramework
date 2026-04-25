package com.tekmoon.domain.util

import androidx.compose.runtime.LaunchedEffect

@androidx.compose.runtime.Composable
actual fun <T> CollectEvents(
    flow: kotlinx.coroutines.flow.Flow<T>,
    key1: Any?,
    key2: Any?,
    onEvent: (T) -> Unit
) {
    LaunchedEffect(flow, key1, key2) {
        flow.collect(onEvent)
    }
}