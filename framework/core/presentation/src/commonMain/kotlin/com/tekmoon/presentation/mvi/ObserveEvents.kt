package com.tekmoon.presentation.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Collects one-shot UI events from a [Flow] and forwards them to [onEvent].
 * Pauses collection while [isActive] is false. Use in a screen's internal composable
 * to react to navigation, toasts, etc. KMP-safe (no platform lifecycle dependency).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun <E> ObserveEvents(
    events: Flow<E>,
    isActive: Boolean = true,
    key: Any? = null,
    onEvent: suspend (E) -> Unit,
) {
    val latestIsActive = rememberUpdatedState(isActive)
    val latestOnEvent = rememberUpdatedState(onEvent)

    LaunchedEffect(events, key) {
        snapshotFlow { latestIsActive.value }
            .flatMapLatest { active -> if (active) events else emptyFlow() }
            .collectLatest { event -> latestOnEvent.value(event) }
    }
}
