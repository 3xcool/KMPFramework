package com.tekmoon.domain.util.paging

import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result

// ---------------------------------------------------------------------------
// Shared domain model used across all paging tests
// ---------------------------------------------------------------------------

data class TestItem(
    val id: String,
    val name: String = id,
    val updatedAt: Long = 0L,
)

// ---------------------------------------------------------------------------
// FakePagingSource — fully controllable test double
// ---------------------------------------------------------------------------

/**
 * Configurable [PagingSource] for testing.
 *
 * @param initialLocalItems  Items returned by [loadLocalItems] (replaced by [clearLocal]).
 * @param remotePages        Map of 1-based page number → page items. Missing pages return [].
 * @param loadPolicy         Policy under test.
 * @param pageSize           Page size (mirrors what [PagingConfig] uses).
 * @param shouldFailRemote   When `true`, [loadRemotePage] returns [DataError.Remote.NO_INTERNET].
 */
class FakePagingSource(
    initialLocalItems: List<TestItem> = emptyList(),
    private val remotePages: Map<Int, List<TestItem>> = emptyMap(),
    override val loadPolicy: PagingLoadPolicy = PagingLoadPolicy.RemoteOnly,
    pageSize: Int = 3,
    var shouldFailRemote: Boolean = false,
) : PagingSource<TestItem, String> {

    override val config = PagingConfig(
        pageSize = pageSize,
        extractKey = { it.id },
        getTimestamp = { it.updatedAt },
    )

    private var localItems: List<TestItem> = initialLocalItems.toList()

    // Observation counters — lets tests verify call counts and side-effects.
    var loadRemoteCallCount = 0; private set
    var loadLocalCallCount = 0;  private set
    val savedItems = mutableListOf<TestItem>()
    var clearLocalCalled = false; private set

    override suspend fun loadRemotePage(page: Int, pageSize: Int): Result<List<TestItem>, DataError.Remote> {
        loadRemoteCallCount++
        return if (shouldFailRemote) {
            Result.Failure(DataError.Remote.NO_INTERNET)
        } else {
            Result.Success(remotePages[page] ?: emptyList())
        }
    }

    override suspend fun loadLocalItems(): List<TestItem> {
        loadLocalCallCount++
        return localItems.toList()
    }

    override suspend fun saveLocal(items: List<TestItem>) {
        savedItems.clear()
        savedItems.addAll(items)
    }

    override suspend fun clearLocal() {
        clearLocalCalled = true
        localItems = emptyList()
    }

    /** Allow tests to update local state mid-test (e.g. after saveLocal side-effects). */
    fun setLocalItems(items: List<TestItem>) {
        localItems = items.toList()
    }
}
