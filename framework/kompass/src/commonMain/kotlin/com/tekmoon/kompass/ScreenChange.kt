package com.tekmoon.kompass

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow

/**
 * A change in the top-most navigation destination — emitted by [NavController.screenChanges]
 * whenever the back stack's top entry transitions to a different [destinationId].
 *
 * @param current the [destinationId] of the new top entry
 * @param previous the previous top entry's id, or `null` on the very first emission
 *   (cold-start landing) or after the back stack is fully cleared
 */
data class ScreenChange(
    val current: String,
    val previous: String?,
)

/**
 * Emits a [ScreenChange] every time the top of the back stack switches to a different
 * destination. Consecutive entries with the same `destinationId` (e.g. when only the
 * `scopeId` changes) are coalesced — only true destination transitions are emitted.
 *
 * The first emission represents the initial landing screen with `previous = null`.
 * Empty back-stack states (the post-corruption fallback in [NavigationState]) are
 * skipped rather than emitted as a malformed change.
 *
 * Typical usage from inside a composable:
 * ```kotlin
 * LaunchedEffect(navController) {
 *     navController.screenChanges().collect { change ->
 *         analytics.screen(change.current, mapOf("from" to change.previous))
 *     }
 * }
 * ```
 *
 * The framework's `TrackScreenViews` helper in `:framework:sdk` wraps this pattern so
 * callers don't have to write the `LaunchedEffect` themselves.
 */
fun NavController.screenChanges(): Flow<ScreenChange> = flow {
    var previous: String? = null
    snapshotFlow { state.backStack.lastOrNull()?.destinationId }
        .filterNotNull()
        .distinctUntilChanged()
        .collect { current ->
            emit(ScreenChange(current = current, previous = previous))
            previous = current
        }
}
