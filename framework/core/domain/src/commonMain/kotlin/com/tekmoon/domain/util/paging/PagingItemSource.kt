package com.tekmoon.domain.util.paging

/**
 * Indicates where a [PagingItem]'s data was sourced from.
 *
 * The UI can use this to show freshness indicators — e.g. a "synced" icon
 * for [Remote] items and a "pending / offline" icon for [Local] items.
 */
enum class PagingItemSource {

    /**
     * The item was loaded from the local cache.
     *
     * This includes:
     * - Items shown before the remote response arrives ([PagingLoadPolicy.LocalThenRemote])
     * - Items that were modified offline and whose local timestamp is newer than remote
     * - Items that exist only locally (pending sync)
     */
    Local,

    /**
     * The item was confirmed by the remote server in this session and won
     * the conflict resolution (remote timestamp >= local timestamp).
     */
    Remote,
}
