package com.tekmoon.domain.util.paging

/**
 * Determines the data-fetching strategy for a [Paginator].
 *
 * Mirrors the philosophy of `DataSessionLoadPolicy` but applied to paginated lists.
 */
enum class PagingLoadPolicy {

    /**
     * Always fetch from the remote source.
     * The local store is never read or written.
     *
     * Use for: search results, live rankings, real-time feeds.
     */
    RemoteOnly,

    /**
     * Only read from the local store; never touch the network.
     *
     * Use for: offline mode, local-only drafts.
     */
    LocalOnly,

    /**
     * Emit local items immediately, then fetch from remote and merge.
     * Remote data is persisted locally after each successful page fetch.
     *
     * Use for: cached feeds, contact lists — show fast, update in background.
     */
    LocalThenRemote,

    /**
     * Fetch from remote first. Falls back to local items only if the remote
     * request fails on the initial load.
     *
     * Use for: critical data where remote is source of truth but offline
     * graceful degradation is still required.
     */
    RemoteThenLocal,
}
