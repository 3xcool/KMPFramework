package com.tekmoon.domain.util.paging

/**
 * Configuration for a [Paginator] and [PagingMerger].
 *
 * @param T the domain model type
 * @param K the stable key type used to identify items (e.g. `String`, `Int`)
 *
 * @param pageSize      Number of items to request per page. Default: 20.
 * @param extractKey    Extracts the stable unique identifier from an item.
 *                      Used for deduplication and conflict resolution.
 * @param getTimestamp  Extracts the UTC epoch-millis timestamp from an item.
 *                      The [PagingMerger] uses this to decide which version of a
 *                      conflicting item is more recent. Items modified offline
 *                      should carry a local timestamp newer than the server value.
 * @param resolveConflict Optional override for conflict resolution.
 *                      When provided, this lambda is called instead of the
 *                      default timestamp comparison whenever two items share the
 *                      same key. Return the item that should be kept.
 *                      When null (default), the item with the newer [getTimestamp]
 *                      wins; on a tie the remote item wins.
 */
data class PagingConfig<T, K>(
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val extractKey: (T) -> K,
    val getTimestamp: (T) -> Long,
    val resolveConflict: ((local: T, remote: T) -> T)? = null,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
