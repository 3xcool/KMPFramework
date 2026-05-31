package com.tekmoon.domain.util.paging

import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * State holder that drives a [PagingSource]. Owns a [StateFlow] of [PagingState]
 * and exposes read operations (load / refresh / retry / reset) and in-memory write
 * operations (insert / update / remove).
 *
 * ### Concurrency
 * All state-mutating operations that involve network or disk I/O are serialised
 * through an internal [Mutex] so rapid user interactions (e.g. quickly scrolling
 * near the end of a list) never produce race conditions or duplicate page requests.
 *
 * Write operations ([insertItem], [updateItem], etc.) are intentionally lock-free:
 * they apply instantly to the in-memory list without touching I/O.
 *
 * ### Lifecycle
 * Pass a [CoroutineScope] tied to the host lifecycle (e.g. `viewModelScope`).
 * The [Paginator] itself is stateless across host recreation — keep one instance
 * per feature scope (e.g. inside a `CommonViewModel`).
 *
 * ### Usage
 *
 * ```kotlin
 * class PostsViewModel(source: PostsPagingSource) : CommonViewModel() {
 *
 *     private val paginator = Paginator(source = source, scope = viewModelScope)
 *     val pagingState: StateFlow<PagingState<Post>> = paginator.state
 *
 *     override suspend fun setup() { paginator.loadInitial() }
 *
 *     fun onScrolledNearBottom() { paginator.loadMore() }
 *     fun onPullToRefresh()      { paginator.refresh() }
 *
 *     // After server confirms a new post was created:
 *     fun onPostCreated(post: Post) {
 *         paginator.insertItem(post) { 0 } // prepend
 *     }
 * }
 * ```
 *
 * @param source  Feature-provided [PagingSource].
 * @param scope   Coroutine scope that owns all launched coroutines.
 * @param merger  Conflict-resolution engine. Defaults to a [PagingMerger] built
 *                from [source]'s config. Override to inject a custom merger in tests.
 */
class Paginator<T, K>(
    private val source: PagingSource<T, K>,
    private val scope: CoroutineScope,
    private val merger: PagingMerger<T, K> = PagingMerger(source.config),
) {

    private val _state = MutableStateFlow(PagingState<T>())
    val state: StateFlow<PagingState<T>> = _state.asStateFlow()

    private val mutex = Mutex()

    /** 1-based index of the next page to fetch from remote. */
    private var nextPage = FIRST_PAGE

    /** Tracks which high-level operation last failed so [retry] can replay it. */
    private var pendingRetryOp: RetryOp = RetryOp.None

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Loads the first page according to [PagingSource.loadPolicy].
     *
     * No-op if items are already loaded — call [refresh] to force a reload.
     */
    fun loadInitial() {
        scope.launch {
            mutex.withLock {
                if (_state.value.items.isNotEmpty() || _state.value.isInitialLoading) return@withLock
                doLoadInitial()
            }
        }
    }

    /**
     * Loads the next page and appends to the current list.
     *
     * No-op if [PagingState.canLoadMore] is `false`.
     */
    fun loadMore() {
        scope.launch {
            mutex.withLock {
                if (!_state.value.canLoadMore) return@withLock
                doLoadMore()
            }
        }
    }

    /**
     * Discards the current list and reloads from page 1.
     * Keeps [PagingState.items] visible during the refresh so the UI does not flash empty.
     */
    fun refresh() {
        scope.launch {
            mutex.withLock {
                doRefresh()
            }
        }
    }

    /**
     * Re-executes the last failed operation (initial load, load-more, or refresh).
     * No-op if there is nothing to retry.
     */
    fun retry() {
        scope.launch {
            mutex.withLock {
                when (pendingRetryOp) {
                    RetryOp.Initial -> doLoadInitial()
                    RetryOp.More    -> doLoadMore()
                    RetryOp.Refresh -> doRefresh()
                    RetryOp.None    -> Unit
                }
            }
        }
    }

    /**
     * Clears everything back to the initial empty state and, for policies that
     * involve local storage, also clears the local cache.
     *
     * Useful when changing the query context (e.g. switching user, applying a filter).
     */
    fun reset() {
        scope.launch {
            mutex.withLock {
                if (source.loadPolicy != PagingLoadPolicy.RemoteOnly) {
                    source.clearLocal()
                }
                nextPage = FIRST_PAGE
                pendingRetryOp = RetryOp.None
                _state.value = PagingState()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Write operations — lock-free in-memory mutations (call after server confirms)
    // -------------------------------------------------------------------------

    /**
     * Inserts [item] into the in-memory list at the position returned by [position].
     *
     * Call this after the server confirms a successful create operation.
     * The [position] lambda receives the current items list and should return
     * the 0-based index at which [item] will be inserted.
     *
     * ```kotlin
     * paginator.insertItem(newPost) { 0 }           // prepend
     * paginator.insertItem(newPost) { it.size }     // append
     * paginator.insertItem(newPost) { items ->      // sorted insert (date desc)
     *     items.indexOfFirst { it.data.date < newPost.date }.takeIf { it >= 0 } ?: items.size
     * }
     * ```
     */
    fun insertItem(item: T, position: (List<PagingItem<T>>) -> Int) {
        _state.update { current ->
            val index = position(current.items).coerceIn(0, current.items.size)
            val pagingItem = PagingItem(data = item, source = PagingItemSource.Remote)
            val newItems = current.items.toMutableList().also { it.add(index, pagingItem) }
            current.copy(items = newItems)
        }
    }

    /**
     * Finds the **first** item matching [predicate], applies [transform] to it,
     * and replaces it in the list. No-op if no item matches.
     */
    fun updateItem(predicate: (T) -> Boolean, transform: (T) -> T) {
        _state.update { current ->
            var updated = false
            val newItems = current.items.map { pagingItem ->
                if (!updated && predicate(pagingItem.data)) {
                    updated = true
                    pagingItem.copy(data = transform(pagingItem.data))
                } else {
                    pagingItem
                }
            }
            current.copy(items = newItems)
        }
    }

    /**
     * Applies [transform] to **all** items matching [predicate].
     *
     * Useful for batch operations (e.g. "mark all as read").
     */
    fun updateItems(predicate: (T) -> Boolean, transform: (T) -> T) {
        _state.update { current ->
            val newItems = current.items.map { pagingItem ->
                if (predicate(pagingItem.data)) pagingItem.copy(data = transform(pagingItem.data))
                else pagingItem
            }
            current.copy(items = newItems)
        }
    }

    /**
     * Removes the **first** item matching [predicate] from the list.
     * No-op if no item matches.
     */
    fun removeItem(predicate: (T) -> Boolean) {
        _state.update { current ->
            var removed = false
            val newItems = current.items.filter { pagingItem ->
                if (!removed && predicate(pagingItem.data)) { removed = true; false }
                else true
            }
            current.copy(items = newItems)
        }
    }

    /**
     * Removes **all** items matching [predicate] from the list.
     */
    fun removeItems(predicate: (T) -> Boolean) {
        _state.update { current ->
            current.copy(items = current.items.filter { !predicate(it.data) })
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers — must be called while holding the mutex
    // -------------------------------------------------------------------------

    private suspend fun doLoadInitial() {
        _state.update { it.copy(isInitialLoading = true, error = null) }
        pendingRetryOp = RetryOp.None

        when (source.loadPolicy) {
            PagingLoadPolicy.RemoteOnly -> {
                fetchRemoteAndApply(
                    page = FIRST_PAGE,
                    existingItems = emptyList(),
                    onSuccess = { merged, hasMore ->
                        _state.update {
                            it.copy(items = merged, isInitialLoading = false, hasReachedEnd = !hasMore, error = null)
                        }
                        nextPage = FIRST_PAGE + 1
                    },
                    onFailure = { error ->
                        _state.update { it.copy(isInitialLoading = false, error = error) }
                        pendingRetryOp = RetryOp.Initial
                    },
                )
            }

            PagingLoadPolicy.LocalOnly -> {
                val wrapped = source.loadLocalItems().map { PagingItem(it, PagingItemSource.Local) }
                _state.update {
                    it.copy(items = wrapped, isInitialLoading = false, hasReachedEnd = true, error = null)
                }
            }

            PagingLoadPolicy.LocalThenRemote -> {
                // Phase 1 — show local immediately.
                val localWrapped = source.loadLocalItems().map { PagingItem(it, PagingItemSource.Local) }
                _state.update { it.copy(items = localWrapped, isInitialLoading = false, error = null) }

                // Phase 2 — fetch remote, merge, persist.
                fetchRemoteAndApply(
                    page = FIRST_PAGE,
                    existingItems = localWrapped,
                    onSuccess = { merged, hasMore ->
                        _state.update {
                            it.copy(items = merged, isInitialLoading = false, hasReachedEnd = !hasMore, error = null)
                        }
                        nextPage = FIRST_PAGE + 1
                        source.saveLocal(merged.map { it.data })
                    },
                    onFailure = { error ->
                        // Local items already visible — just surface the error without clearing list.
                        _state.update { it.copy(error = error) }
                        pendingRetryOp = RetryOp.Initial
                    },
                )
            }

            PagingLoadPolicy.RemoteThenLocal -> {
                // Try remote; fall back to local only if remote fails.
                fetchRemoteAndApply(
                    page = FIRST_PAGE,
                    existingItems = emptyList(),
                    onSuccess = { merged, hasMore ->
                        _state.update {
                            it.copy(items = merged, isInitialLoading = false, hasReachedEnd = !hasMore, error = null)
                        }
                        nextPage = FIRST_PAGE + 1
                        source.saveLocal(merged.map { it.data })
                    },
                    onFailure = { error ->
                        val localWrapped = source.loadLocalItems().map { PagingItem(it, PagingItemSource.Local) }
                        if (localWrapped.isNotEmpty()) {
                            _state.update {
                                it.copy(items = localWrapped, isInitialLoading = false, hasReachedEnd = true, error = error)
                            }
                        } else {
                            _state.update { it.copy(isInitialLoading = false, error = error) }
                            pendingRetryOp = RetryOp.Initial
                        }
                    },
                )
            }
        }
    }

    private suspend fun doLoadMore() {
        _state.update { it.copy(isLoadingMore = true, error = null) }
        pendingRetryOp = RetryOp.None

        // Snapshot current items before the async fetch so the merge baseline is stable.
        val currentItems = _state.value.items

        fetchRemoteAndApply(
            page = nextPage,
            existingItems = currentItems,
            onSuccess = { merged, hasMore ->
                _state.update {
                    it.copy(items = merged, isLoadingMore = false, hasReachedEnd = !hasMore, error = null)
                }
                nextPage++
                if (source.loadPolicy != PagingLoadPolicy.RemoteOnly) {
                    source.saveLocal(merged.map { it.data })
                }
            },
            onFailure = { error ->
                _state.update { it.copy(isLoadingMore = false, error = error) }
                pendingRetryOp = RetryOp.More
            },
        )
    }

    private suspend fun doRefresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        pendingRetryOp = RetryOp.None

        when (source.loadPolicy) {
            PagingLoadPolicy.RemoteOnly -> {
                fetchRemoteAndApply(
                    page = FIRST_PAGE,
                    existingItems = emptyList(),
                    onSuccess = { merged, hasMore ->
                        _state.update {
                            it.copy(items = merged, isRefreshing = false, hasReachedEnd = !hasMore, error = null)
                        }
                        nextPage = FIRST_PAGE + 1
                    },
                    onFailure = { error ->
                        _state.update { it.copy(isRefreshing = false, error = error) }
                        pendingRetryOp = RetryOp.Refresh
                    },
                )
            }

            PagingLoadPolicy.LocalOnly -> {
                // Clear cache and re-read local.
                source.clearLocal()
                val wrapped = source.loadLocalItems().map { PagingItem(it, PagingItemSource.Local) }
                _state.update {
                    it.copy(items = wrapped, isRefreshing = false, hasReachedEnd = true, error = null)
                }
                nextPage = FIRST_PAGE
            }

            PagingLoadPolicy.LocalThenRemote -> {
                // Clear cache first, then reload: local (empty after clear) + remote.
                source.clearLocal()
                val localWrapped = source.loadLocalItems().map { PagingItem(it, PagingItemSource.Local) }
                _state.update { it.copy(items = localWrapped, error = null) }

                fetchRemoteAndApply(
                    page = FIRST_PAGE,
                    existingItems = localWrapped,
                    onSuccess = { merged, hasMore ->
                        _state.update {
                            it.copy(items = merged, isRefreshing = false, hasReachedEnd = !hasMore, error = null)
                        }
                        nextPage = FIRST_PAGE + 1
                        source.saveLocal(merged.map { it.data })
                    },
                    onFailure = { error ->
                        _state.update { it.copy(isRefreshing = false, error = error) }
                        pendingRetryOp = RetryOp.Refresh
                    },
                )
            }

            PagingLoadPolicy.RemoteThenLocal -> {
                // Do NOT clear local before the fetch — local is the fallback if remote fails.
                fetchRemoteAndApply(
                    page = FIRST_PAGE,
                    existingItems = emptyList(),
                    onSuccess = { merged, hasMore ->
                        // Remote succeeded: now safe to replace local cache.
                        source.clearLocal()
                        source.saveLocal(merged.map { it.data })
                        _state.update {
                            it.copy(items = merged, isRefreshing = false, hasReachedEnd = !hasMore, error = null)
                        }
                        nextPage = FIRST_PAGE + 1
                    },
                    onFailure = { error ->
                        // Remote failed: keep current (stale) items in view, surface the error.
                        _state.update { it.copy(isRefreshing = false, error = error) }
                        pendingRetryOp = RetryOp.Refresh
                    },
                )
            }
        }
    }

    /**
     * Fetches one remote page and invokes [onSuccess] or [onFailure].
     * Does **not** mutate [_state] directly — callers own state transitions.
     */
    private suspend fun fetchRemoteAndApply(
        page: Int,
        existingItems: List<PagingItem<T>>,
        onSuccess: suspend (merged: List<PagingItem<T>>, hasMore: Boolean) -> Unit,
        onFailure: suspend (DataError) -> Unit,
    ) {
        val pageSize = source.config.pageSize
        when (val result = source.loadRemotePage(page, pageSize)) {
            is Result.Success -> {
                val remoteItems = result.data
                val merged = merger.merge(
                    existing = existingItems,
                    incoming = remoteItems,
                    incomingSource = PagingItemSource.Remote,
                )
                onSuccess(merged, remoteItems.size >= pageSize)
            }
            is Result.Failure -> onFailure(result.error)
        }
    }

    // -------------------------------------------------------------------------
    // Internal types
    // -------------------------------------------------------------------------

    private enum class RetryOp { None, Initial, More, Refresh }

    companion object {
        private const val FIRST_PAGE = 1
    }
}
