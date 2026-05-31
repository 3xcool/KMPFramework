package com.tekmoon.domain.util.paging

import com.tekmoon.domain.util.data.DataError

/**
 * Reactive state owned by a [Paginator]. Designed to be collected directly
 * from the UI layer.
 *
 * Items are wrapped in [PagingItem] so the UI knows whether each entry came
 * from the local cache or was confirmed by the remote server (see [PagingItemSource]).
 *
 * ### Loading-flag semantics
 *
 * | flag              | list visible? | meaning                                           |
 * |-------------------|:---:|---------------------------------------------------|
 * | [isInitialLoading] | no  | First page in flight; [items] is still empty      |
 * | [isRefreshing]    | yes | Pull-to-refresh; [items] shows previous content   |
 * | [isLoadingMore]   | yes | Next page appending; [items] shows current content|
 * | [hasReachedEnd]   | yes | Remote returned fewer items than [PagingConfig.pageSize]; no more pages |
 *
 * At most one of the three loading flags is `true` at any given time.
 */
data class PagingState<T>(
    val items: List<PagingItem<T>> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val error: DataError? = null,
) {
    /**
     * `true` when there are no items and no load is in progress.
     * Useful for showing an empty-state placeholder.
     */
    val isEmpty: Boolean
        get() = items.isEmpty() && !isInitialLoading && !isLoadingMore && !isRefreshing

    /**
     * `true` when it is safe and meaningful to call [Paginator.loadMore].
     */
    val canLoadMore: Boolean
        get() = !isInitialLoading && !isLoadingMore && !isRefreshing && !hasReachedEnd && error == null
}
