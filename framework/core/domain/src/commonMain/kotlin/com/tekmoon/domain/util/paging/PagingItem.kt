package com.tekmoon.domain.util.paging

/**
 * Wraps a domain model with paging metadata.
 *
 * [PagingState.items] is a `List<PagingItem<T>>` so the UI always has access
 * to [source] without needing a separate lookup.
 *
 * ### Example
 *
 * ```kotlin
 * state.items.forEach { pagingItem ->
 *     Row {
 *         MyItemView(data = pagingItem.data)
 *         if (pagingItem.source == PagingItemSource.Local) {
 *             PendingSyncIcon()
 *         }
 *     }
 * }
 * ```
 */
data class PagingItem<out T>(
    val data: T,
    val source: PagingItemSource,
)
