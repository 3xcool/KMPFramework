package com.tekmoon.domain.util.paging

/**
 * Merges two lists of items using the rules defined in [PagingConfig].
 *
 * The framework uses this internally inside [Paginator], but it is public so
 * callers can reuse or test the merge logic independently.
 *
 * ### Merge rules
 *
 * For each item in [incoming]:
 * - If no item with the same key exists in [existing] → add as-is tagged with [incomingSource].
 * - If a matching item exists:
 *   - **True local ↔ remote conflict** (one side is [PagingItemSource.Local], the other
 *     [PagingItemSource.Remote]):
 *     - If [PagingConfig.resolveConflict] is provided → call it; use `===` reference
 *       equality to determine which argument was returned and tag the winner accordingly.
 *       If the lambda returns a blended/new object, [incomingSource] is used as the tag.
 *     - Otherwise fall through to timestamp comparison.
 *   - **Same-source conflict** (both remote or both local — e.g. duplicate keys across pages):
 *     - Always use timestamp comparison.
 *   - **Timestamp comparison**: the item with the strictly newer UTC wins; on a tie
 *     the existing item is preserved (protects offline modifications).
 *
 * Items in [existing] that do **not** appear in [incoming] are always preserved
 * (they may be on a different page or modified offline and pending sync).
 *
 * ### Deduplication within [incoming]
 *
 * If [incoming] itself contains duplicate keys, the last occurrence wins.
 */
class PagingMerger<T, K>(private val config: PagingConfig<T, K>) {

    /**
     * Merges [incoming] items into [existing], returning the combined list.
     *
     * @param existing       Current items already loaded (may be empty on first load).
     * @param incoming       New items arriving from the source (remote or local snapshot).
     * @param incomingSource The [PagingItemSource] to tag items from [incoming] that win.
     */
    fun merge(
        existing: List<PagingItem<T>>,
        incoming: List<T>,
        incomingSource: PagingItemSource,
    ): List<PagingItem<T>> {
        if (incoming.isEmpty()) return existing

        // LinkedHashMap preserves insertion order (keeps existing item order stable).
        val resultMap = LinkedHashMap<K, PagingItem<T>>(existing.size + incoming.size)
        existing.forEach { pagingItem ->
            resultMap[config.extractKey(pagingItem.data)] = pagingItem
        }

        incoming.forEach { incomingItem ->
            val key = config.extractKey(incomingItem)
            val existingPagingItem = resultMap[key]

            resultMap[key] = if (existingPagingItem == null) {
                PagingItem(data = incomingItem, source = incomingSource)
            } else {
                resolve(existingPagingItem, incomingItem, incomingSource)
            }
        }

        return resultMap.values.toList()
    }

    private fun resolve(
        existingPagingItem: PagingItem<T>,
        incomingItem: T,
        incomingSource: PagingItemSource,
    ): PagingItem<T> {
        val existingSource = existingPagingItem.source
        val customResolve = config.resolveConflict

        // Only invoke custom resolver for true local ↔ remote conflicts.
        val isCrossSourceConflict =
            (existingSource == PagingItemSource.Local && incomingSource == PagingItemSource.Remote) ||
            (existingSource == PagingItemSource.Remote && incomingSource == PagingItemSource.Local)

        if (isCrossSourceConflict && customResolve != null) {
            val (localItem, remoteItem) = if (existingSource == PagingItemSource.Local) {
                existingPagingItem.data to incomingItem
            } else {
                incomingItem to existingPagingItem.data
            }
            val winner = customResolve(localItem, remoteItem)
            // Use reference equality: the lambda should return one of its two arguments.
            // If it returns a brand-new blended object, we fall back to incomingSource.
            val winnerSource = when {
                winner === localItem  -> PagingItemSource.Local
                winner === remoteItem -> PagingItemSource.Remote
                else                  -> incomingSource
            }
            return PagingItem(data = winner, source = winnerSource)
        }

        // Default: newer timestamp wins; tie preserves existing (protects offline edits).
        val existingTs = config.getTimestamp(existingPagingItem.data)
        val incomingTs = config.getTimestamp(incomingItem)
        return if (incomingTs > existingTs) {
            PagingItem(data = incomingItem, source = incomingSource)
        } else {
            existingPagingItem
        }
    }
}
