package com.tekmoon.domain.util.paging

import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result
import com.tekmoon.domain.util.remote.RetryPolicy
import com.tekmoon.domain.util.remote.withRetry

/**
 * A [PagingSource] that automatically wraps remote page fetches with [withRetry].
 *
 * Implementors override [fetchPage] instead of [loadRemotePage].  The retry
 * loop is sealed inside this class so every subclass gets retry-with-back-off
 * for free without any boilerplate.
 *
 * Example:
 * ```kotlin
 * class MyPagingSource(api: MyApi) : RetryablePagingSource<Post, String>(
 *     retryPolicy = RetryPolicy(maxRetries = 3)
 * ) {
 *     override val config = PagingConfig(extractKey = { it.id }, getTimestamp = { it.updatedAt })
 *     override val loadPolicy = PagingLoadPolicy.RemoteThenLocal
 *
 *     override suspend fun fetchPage(page: Int, pageSize: Int) =
 *         api.getPosts(page, pageSize)
 *
 *     override suspend fun loadLocalItems() = db.getAllPosts()
 *     override suspend fun saveLocal(items: List<Post>) = db.upsert(items)
 *     override suspend fun clearLocal() = db.clearAll()
 * }
 * ```
 *
 * @param retryPolicy Controls how many times and how long to retry transient failures.
 *                    Defaults to [RetryPolicy] (3 retries, exponential back-off from 1 s).
 */
abstract class RetryablePagingSource<T, K>(
    val retryPolicy: RetryPolicy = RetryPolicy(),
) : PagingSource<T, K> {

    /**
     * Perform the actual network request for a single page.
     *
     * Do NOT call [withRetry] here — it is applied automatically by [loadRemotePage].
     *
     * @param page     1-indexed page number.
     * @param pageSize Number of items per page (from [PagingConfig.pageSize]).
     * @return [Result.Success] with the page items, or [Result.Failure] with a [DataError].
     */
    abstract suspend fun fetchPage(page: Int, pageSize: Int): Result<List<T>, DataError>

    /**
     * Sealed — do not override in subclasses.  Delegates to [fetchPage] wrapped by [withRetry].
     */
    final override suspend fun loadRemotePage(
        page: Int,
        pageSize: Int,
    ): Result<List<T>, DataError> = withRetry(retryPolicy) { fetchPage(page, pageSize) }
}
