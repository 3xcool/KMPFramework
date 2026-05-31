package com.tekmoon.domain.util.paging

import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result

/**
 * Feature-provided contract that knows how to fetch and persist a paginated list.
 *
 * [Paginator] stays generic and does not know anything about repositories,
 * database schemas, or network clients — the caller owns all of that here.
 *
 * ### Example
 *
 * ```kotlin
 * class PostsPagingSource(
 *     private val api: PostsApi,
 *     private val db: PostsQueries,
 * ) : PagingSource<Post, String> {
 *
 *     override val config = PagingConfig(
 *         pageSize = 20,
 *         extractKey = { it.id },
 *         getTimestamp = { it.updatedAtUtc },
 *     )
 *
 *     override val loadPolicy = PagingLoadPolicy.LocalThenRemote
 *
 *     override suspend fun loadRemotePage(page: Int, pageSize: Int) =
 *         api.getPosts(page, pageSize)
 *
 *     override suspend fun loadLocalItems(): List<Post> =
 *         db.selectAll().executeAsList().map { it.toDomain() }
 *
 *     override suspend fun saveLocal(items: List<Post>) =
 *         db.transaction { items.forEach { db.upsert(it.toEntity()) } }
 *
 *     override suspend fun clearLocal() = db.deleteAll()
 * }
 * ```
 *
 * @param T the domain model type
 * @param K the stable key type returned by [PagingConfig.extractKey]
 */
interface PagingSource<T, K> {

    /**
     * Configuration shared with [PagingMerger] and [Paginator].
     */
    val config: PagingConfig<T, K>

    /**
     * The loading strategy this source uses.
     */
    val loadPolicy: PagingLoadPolicy

    /**
     * Fetches a single page of items from the remote server.
     *
     * @param page     1-based page number.
     * @param pageSize Number of items to request (mirrors [PagingConfig.pageSize]).
     * @return [Result.Success] with the page items, or [Result.Failure] with a [DataError.Remote].
     *         An empty list signals the last page.
     */
    suspend fun loadRemotePage(page: Int, pageSize: Int): Result<List<T>, DataError>

    /**
     * Returns a snapshot of all currently cached items from the local store.
     *
     * Called once during [Paginator.loadInitial] and [Paginator.refresh] for policies
     * that involve local data ([PagingLoadPolicy.LocalOnly], [PagingLoadPolicy.LocalThenRemote],
     * [PagingLoadPolicy.RemoteThenLocal]).
     *
     * Default implementation returns an empty list (no-op for [PagingLoadPolicy.RemoteOnly]).
     */
    suspend fun loadLocalItems(): List<T> = emptyList()

    /**
     * Persists the merged winner items to the local store.
     *
     * Called after each successful remote page fetch with the **merged result**
     * (items after conflict resolution), not the raw remote response.
     * The caller should implement this as a simple upsert — no timestamp
     * comparison is needed here because the framework already resolved conflicts.
     *
     * Default implementation is a no-op (suitable for [PagingLoadPolicy.RemoteOnly]).
     */
    suspend fun saveLocal(items: List<T>) {}

    /**
     * Clears all locally cached items.
     *
     * Called during [Paginator.reset] and at the start of [Paginator.refresh]
     * for policies that involve local data.
     *
     * Default implementation is a no-op.
     */
    suspend fun clearLocal() {}
}
