package com.tekmoon.domain.util

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow


@Composable
expect fun <T> CollectEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
)