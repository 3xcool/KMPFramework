package com.tekmoon.domain.util.paging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [Paginator].
 *
 * Each test case drives the paginator through a [PaginatorTestRobot] so the
 * assertions read like plain English and coroutine draining is encapsulated.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaginatorTest {

    // -----------------------------------------------------------------------
    // loadInitial — RemoteOnly
    // -----------------------------------------------------------------------

    @Test
    fun loadInitial_remoteOnly_happyPath_itemsAreRemoteTagged() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"), TestItem("b"), TestItem("c"))),
            pageSize = 3,
        )
        robot.loadInitial()

        robot.assertItemCount(3)
        robot.assertItem(0, "a")
        robot.assertAllSources(PagingItemSource.Remote)
        robot.assertNotLoading()
        robot.assertNoError()
    }

    @Test
    fun loadInitial_remoteOnly_errorSetsErrorState() = runTest {
        val robot = robot(policy = PagingLoadPolicy.RemoteOnly, shouldFailRemote = true)
        robot.loadInitial()

        robot.assertItemCount(0)
        robot.assertHasError()
        robot.assertNotLoading()
    }

    @Test
    fun loadInitial_remoteOnly_isNoOpIfItemsAlreadyLoaded() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
        )
        robot.loadInitial()
        robot.loadInitial() // second call — should be no-op

        assertEquals(1, robot.source.loadRemoteCallCount)
    }

    @Test
    fun loadInitial_remoteOnly_pageSmallThanSize_hasReachedEnd() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))), // 1 item < pageSize 3
            pageSize = 3,
        )
        robot.loadInitial()
        robot.assertHasReachedEnd()
    }

    @Test
    fun loadInitial_remoteOnly_fullPage_hasNotReachedEnd() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"), TestItem("b"), TestItem("c"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.assertNotReachedEnd()
    }

    // -----------------------------------------------------------------------
    // loadInitial — LocalOnly
    // -----------------------------------------------------------------------

    @Test
    fun loadInitial_localOnly_showsLocalItemsTaggedLocal() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.LocalOnly,
            localItems = listOf(TestItem("x"), TestItem("y")),
        )
        robot.loadInitial()

        robot.assertItemCount(2)
        robot.assertAllSources(PagingItemSource.Local)
        robot.assertHasReachedEnd() // local-only never has more pages
        assertEquals(0, robot.source.loadRemoteCallCount)
    }

    // -----------------------------------------------------------------------
    // loadInitial — LocalThenRemote
    // -----------------------------------------------------------------------

    @Test
    fun loadInitial_localThenRemote_emitsLocalFirst_thenMergesRemote() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.LocalThenRemote,
            localItems = listOf(TestItem("a", updatedAt = 1L)),
            remotePages = mapOf(1 to listOf(
                TestItem("a", name = "server-a", updatedAt = 2L), // remote is newer
                TestItem("b", updatedAt = 1L),
            )),
            pageSize = 3,
        )
        robot.loadInitial()

        robot.assertItemCount(2)
        // "a" — remote was newer → remote wins
        robot.assertItem(0, "a")
        assertEquals(PagingItemSource.Remote, robot.itemSource(0))
        assertEquals("server-a", robot.itemName(0))
        // "b" — only in remote
        robot.assertItem(1, "b")
        assertEquals(PagingItemSource.Remote, robot.itemSource(1))
        // saveLocal should have been called
        assertTrue(robot.source.savedItems.isNotEmpty())
    }

    @Test
    fun loadInitial_localThenRemote_localOfflineEdit_winsOverRemote() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.LocalThenRemote,
            localItems = listOf(TestItem("a", name = "offline-edit", updatedAt = 10L)),
            remotePages = mapOf(1 to listOf(TestItem("a", name = "server", updatedAt = 5L))),
            pageSize = 3,
        )
        robot.loadInitial()

        robot.assertItemCount(1)
        assertEquals("offline-edit", robot.itemName(0))
        assertEquals(PagingItemSource.Local, robot.itemSource(0))
    }

    @Test
    fun loadInitial_localThenRemote_remoteFails_keepsLocalItemsAndSetsError() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.LocalThenRemote,
            localItems = listOf(TestItem("x")),
            shouldFailRemote = true,
        )
        robot.loadInitial()

        robot.assertItemCount(1) // local items still shown
        robot.assertHasError()
        robot.assertNotLoading()
    }

    // -----------------------------------------------------------------------
    // loadInitial — RemoteThenLocal
    // -----------------------------------------------------------------------

    @Test
    fun loadInitial_remoteThenLocal_remoteSuceeds_showsRemote() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteThenLocal,
            remotePages = mapOf(1 to listOf(TestItem("r1"), TestItem("r2"))),
            pageSize = 3,
        )
        robot.loadInitial()

        robot.assertItemCount(2)
        robot.assertAllSources(PagingItemSource.Remote)
    }

    @Test
    fun loadInitial_remoteThenLocal_remoteFails_fallsBackToLocal() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteThenLocal,
            localItems = listOf(TestItem("cached")),
            shouldFailRemote = true,
        )
        robot.loadInitial()

        robot.assertItemCount(1)
        robot.assertItem(0, "cached")
        assertEquals(PagingItemSource.Local, robot.itemSource(0))
        robot.assertHasError() // error is surfaced even with fallback
        robot.assertHasReachedEnd() // local fallback has no further pages
    }

    @Test
    fun loadInitial_remoteThenLocal_remoteFailsNoLocal_emptyWithError() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteThenLocal,
            shouldFailRemote = true,
        )
        robot.loadInitial()

        robot.assertItemCount(0)
        robot.assertHasError()
    }

    // -----------------------------------------------------------------------
    // loadMore
    // -----------------------------------------------------------------------

    @Test
    fun loadMore_appendsNextPage() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(
                1 to listOf(TestItem("a"), TestItem("b"), TestItem("c")),
                2 to listOf(TestItem("d"), TestItem("e"), TestItem("f")),
            ),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.loadMore()

        robot.assertItemCount(6)
        robot.assertItem(3, "d")
        robot.assertItem(5, "f")
    }

    @Test
    fun loadMore_deduplicatesItemsAlreadyInList() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(
                1 to listOf(TestItem("a"), TestItem("b"), TestItem("c")),
                2 to listOf(TestItem("b"), TestItem("d")), // "b" is a duplicate
            ),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.loadMore()

        // "b" should not be duplicated
        robot.assertItemCount(4)
        assertEquals(1, robot.state.items.count { it.data.id == "b" })
    }

    @Test
    fun loadMore_isNoOpWhenCanLoadMoreIsFalse() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))), // 1 item < pageSize → hasReachedEnd
            pageSize = 3,
        )
        robot.loadInitial()
        robot.assertHasReachedEnd()

        robot.loadMore() // must be no-op
        assertEquals(1, robot.source.loadRemoteCallCount)
    }

    @Test
    fun loadMore_setsHasReachedEndWhenLastPagePartial() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(
                1 to listOf(TestItem("a"), TestItem("b"), TestItem("c")),
                2 to listOf(TestItem("d")), // partial page
            ),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.loadMore()

        robot.assertHasReachedEnd()
    }

    @Test
    fun loadMore_error_setsErrorAndCanRetry() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"), TestItem("b"), TestItem("c"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.source.shouldFailRemote = true
        robot.loadMore()

        robot.assertHasError()
        robot.assertFalseIsLoadingMore()
    }

    // -----------------------------------------------------------------------
    // refresh
    // -----------------------------------------------------------------------

    @Test
    fun refresh_resetsAndReloadsFromPageOne() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(
                1 to listOf(TestItem("a"), TestItem("b"), TestItem("c")),
                2 to listOf(TestItem("d"), TestItem("e"), TestItem("f")),
            ),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.loadMore()
        robot.assertItemCount(6)

        robot.refresh()

        robot.assertItemCount(3) // back to page 1 only
        robot.assertItem(0, "a")
        robot.assertNotLoading()
    }

    @Test
    fun refresh_localThenRemote_clearsThenReloads() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.LocalThenRemote,
            localItems = listOf(TestItem("old")),
            remotePages = mapOf(1 to listOf(TestItem("new1"), TestItem("new2"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.refresh()

        assertTrue(robot.source.clearLocalCalled)
        robot.assertItemCount(2)
        robot.assertItem(0, "new1")
    }

    @Test
    fun refresh_remoteThenLocal_doesNotClearLocalBeforeRemoteFetch() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteThenLocal,
            localItems = listOf(TestItem("fallback")),
            remotePages = mapOf(1 to listOf(TestItem("fresh"))),
            pageSize = 3,
        )
        robot.loadInitial()

        // Simulate network going down before refresh
        robot.source.shouldFailRemote = true
        robot.refresh()

        // Local must NOT have been cleared before the failed remote fetch
        assertFalse(robot.source.clearLocalCalled)
        robot.assertHasError()
    }

    // -----------------------------------------------------------------------
    // retry
    // -----------------------------------------------------------------------

    @Test
    fun retry_afterInitialFailure_replaysLoadInitial() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            shouldFailRemote = true,
            pageSize = 3,
        )
        robot.loadInitial()
        robot.assertHasError()

        robot.source.shouldFailRemote = false
        robot.retry()

        robot.assertItemCount(1)
        robot.assertNoError()
    }

    @Test
    fun retry_afterLoadMoreFailure_replaysLoadMore() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(
                1 to listOf(TestItem("a"), TestItem("b"), TestItem("c")),
                2 to listOf(TestItem("d")),
            ),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.source.shouldFailRemote = true
        robot.loadMore()
        robot.assertHasError()

        robot.source.shouldFailRemote = false
        robot.retry()

        robot.assertItemCount(4)
        robot.assertNoError()
    }

    @Test
    fun retry_afterRefreshFailure_replaysRefresh() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.source.shouldFailRemote = true
        robot.refresh()
        robot.assertHasError()

        robot.source.shouldFailRemote = false
        robot.retry()

        robot.assertItemCount(1)
        robot.assertNoError()
    }

    @Test
    fun retry_noFailure_isNoOp() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        val callsBefore = robot.source.loadRemoteCallCount
        robot.retry()

        assertEquals(callsBefore, robot.source.loadRemoteCallCount)
    }

    // -----------------------------------------------------------------------
    // reset
    // -----------------------------------------------------------------------

    @Test
    fun reset_clearsStateAndCallsClearLocal() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.LocalThenRemote,
            remotePages = mapOf(1 to listOf(TestItem("a"), TestItem("b"), TestItem("c"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.assertItemCount(3)

        robot.reset()

        robot.assertItemCount(0)
        robot.assertNotLoading()
        assertTrue(robot.source.clearLocalCalled)
    }

    @Test
    fun reset_remoteOnly_doesNotCallClearLocal() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.reset()

        assertFalse(robot.source.clearLocalCalled)
    }

    @Test
    fun reset_allowsLoadInitialAgain() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.reset()
        robot.loadInitial()

        robot.assertItemCount(1)
        assertEquals(2, robot.source.loadRemoteCallCount)
    }

    // -----------------------------------------------------------------------
    // Write operations — insertItem
    // -----------------------------------------------------------------------

    @Test
    fun insertItem_prependsAtPositionZero() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("b"), TestItem("c"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.insertItem(TestItem("a")) { 0 }

        robot.assertItemCount(3)
        robot.assertItem(0, "a")
    }

    @Test
    fun insertItem_appendsAtEnd() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"), TestItem("b"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.insertItem(TestItem("c")) { it.size }

        robot.assertItemCount(3)
        robot.assertItem(2, "c")
    }

    @Test
    fun insertItem_sortedPosition() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(
                TestItem("a", updatedAt = 10L),
                TestItem("c", updatedAt = 5L),
            )),
            pageSize = 3,
        )
        robot.loadInitial()
        // Insert "b" between "a" and "c" (sorted by updatedAt desc)
        robot.insertItem(TestItem("b", updatedAt = 7L)) { items ->
            items.indexOfFirst { it.data.updatedAt < 7L }.takeIf { it >= 0 } ?: items.size
        }

        robot.assertItemCount(3)
        robot.assertItem(0, "a")
        robot.assertItem(1, "b")
        robot.assertItem(2, "c")
    }

    @Test
    fun insertItem_taggedAsRemote() = runTest {
        val robot = robot(policy = PagingLoadPolicy.RemoteOnly, pageSize = 3)
        robot.insertItem(TestItem("new")) { 0 }

        assertEquals(PagingItemSource.Remote, robot.itemSource(0))
    }

    // -----------------------------------------------------------------------
    // Write operations — updateItem
    // -----------------------------------------------------------------------

    @Test
    fun updateItem_updatesFirstMatchOnly() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(
                TestItem("a", name = "alpha"),
                TestItem("b", name = "beta"),
                TestItem("a2", name = "alpha2"), // "a2" won't match
            )),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.updateItem(predicate = { it.id == "a" }) { it.copy(name = "UPDATED") }

        assertEquals("UPDATED", robot.itemName(0))
        assertEquals("beta",    robot.itemName(1))
        assertEquals("alpha2",  robot.itemName(2))
    }

    @Test
    fun updateItem_noMatch_isNoOp() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.updateItem(predicate = { it.id == "z" }) { it.copy(name = "x") }

        robot.assertItemCount(1)
        assertEquals("a", robot.itemName(0))
    }

    // -----------------------------------------------------------------------
    // Write operations — updateItems
    // -----------------------------------------------------------------------

    @Test
    fun updateItems_updatesAllMatches() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(
                TestItem("a", name = "read"),
                TestItem("b", name = "unread"),
                TestItem("c", name = "unread"),
            )),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.updateItems(predicate = { it.name == "unread" }) { it.copy(name = "read") }

        assertEquals("read", robot.itemName(0))
        assertEquals("read", robot.itemName(1))
        assertEquals("read", robot.itemName(2))
    }

    // -----------------------------------------------------------------------
    // Write operations — removeItem
    // -----------------------------------------------------------------------

    @Test
    fun removeItem_removesFirstMatchOnly() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(
                TestItem("a"),
                TestItem("b"),
                TestItem("a2"), // different id — won't match "a"
            )),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.removeItem { it.id == "a" }

        robot.assertItemCount(2)
        robot.assertItem(0, "b")
        robot.assertItem(1, "a2")
    }

    @Test
    fun removeItem_noMatch_isNoOp() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.removeItem { it.id == "z" }

        robot.assertItemCount(1)
    }

    // -----------------------------------------------------------------------
    // Write operations — removeItems
    // -----------------------------------------------------------------------

    @Test
    fun removeItems_removesAllMatches() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(
                TestItem("a", name = "keep"),
                TestItem("b", name = "remove"),
                TestItem("c", name = "remove"),
            )),
            pageSize = 3,
        )
        robot.loadInitial()
        robot.removeItems { it.name == "remove" }

        robot.assertItemCount(1)
        robot.assertItem(0, "a")
    }

    // -----------------------------------------------------------------------
    // PagingState computed properties
    // -----------------------------------------------------------------------

    @Test
    fun isEmpty_trueWhenNoItemsAndNotLoading() = runTest {
        val robot = robot(policy = PagingLoadPolicy.RemoteOnly, pageSize = 3)
        assertTrue(robot.state.isEmpty)
    }

    @Test
    fun isEmpty_falseWhenItemsPresent() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))),
            pageSize = 3,
        )
        robot.loadInitial()
        assertFalse(robot.state.isEmpty)
    }

    @Test
    fun canLoadMore_falseWhenHasReachedEnd() = runTest {
        val robot = robot(
            policy = PagingLoadPolicy.RemoteOnly,
            remotePages = mapOf(1 to listOf(TestItem("a"))), // partial page
            pageSize = 3,
        )
        robot.loadInitial()
        assertFalse(robot.state.canLoadMore)
    }

    @Test
    fun canLoadMore_falseWhenErrorPresent() = runTest {
        val robot = robot(policy = PagingLoadPolicy.RemoteOnly, shouldFailRemote = true, pageSize = 3)
        robot.loadInitial()
        assertFalse(robot.state.canLoadMore)
    }

    // -----------------------------------------------------------------------
    // Robot factory
    // -----------------------------------------------------------------------

    private fun TestScope.robot(
        policy: PagingLoadPolicy = PagingLoadPolicy.RemoteOnly,
        localItems: List<TestItem> = emptyList(),
        remotePages: Map<Int, List<TestItem>> = emptyMap(),
        pageSize: Int = 3,
        shouldFailRemote: Boolean = false,
    ) = PaginatorTestRobot(
        scope = this,
        source = FakePagingSource(
            initialLocalItems = localItems,
            remotePages = remotePages,
            loadPolicy = policy,
            pageSize = pageSize,
            shouldFailRemote = shouldFailRemote,
        ),
    )
}

// ---------------------------------------------------------------------------
// PaginatorTestRobot
// ---------------------------------------------------------------------------

/**
 * Robot wrapper for [Paginator] that drains coroutines after each operation
 * and exposes readable assertion helpers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaginatorTestRobot(
    private val scope: TestScope,
    val source: FakePagingSource,
) {
    private val paginator = Paginator(source = source, scope = scope)

    val state get() = paginator.state.value

    // --- Operations ---

    fun loadInitial() { paginator.loadInitial(); scope.advanceUntilIdle() }
    fun loadMore()    { paginator.loadMore();    scope.advanceUntilIdle() }
    fun refresh()     { paginator.refresh();     scope.advanceUntilIdle() }
    fun retry()       { paginator.retry();       scope.advanceUntilIdle() }
    fun reset()       { paginator.reset();       scope.advanceUntilIdle() }

    fun insertItem(item: TestItem, position: (List<PagingItem<TestItem>>) -> Int) =
        paginator.insertItem(item, position)

    fun updateItem(predicate: (TestItem) -> Boolean, transform: (TestItem) -> TestItem) =
        paginator.updateItem(predicate, transform)

    fun updateItems(predicate: (TestItem) -> Boolean, transform: (TestItem) -> TestItem) =
        paginator.updateItems(predicate, transform)

    fun removeItem(predicate: (TestItem) -> Boolean) = paginator.removeItem(predicate)
    fun removeItems(predicate: (TestItem) -> Boolean) = paginator.removeItems(predicate)

    // --- Accessors ---

    fun itemSource(index: Int): PagingItemSource = state.items[index].source
    fun itemName(index: Int): String = state.items[index].data.name

    // --- Assertions ---

    fun assertItemCount(expected: Int) =
        assertEquals(expected, state.items.size, "Expected $expected items, got ${state.items.size}")

    fun assertItem(index: Int, expectedId: String) =
        assertEquals(expectedId, state.items[index].data.id, "Item[$index] id mismatch")

    fun assertAllSources(expected: PagingItemSource) =
        state.items.forEachIndexed { i, item ->
            assertEquals(expected, item.source, "Item[$i] source should be $expected")
        }

    fun assertNotLoading() {
        assertFalse(state.isInitialLoading, "Expected isInitialLoading=false")
        assertFalse(state.isLoadingMore,    "Expected isLoadingMore=false")
        assertFalse(state.isRefreshing,     "Expected isRefreshing=false")
    }

    fun assertFalseIsLoadingMore() =
        assertFalse(state.isLoadingMore, "Expected isLoadingMore=false")

    fun assertHasError()  = assertNotNull(state.error, "Expected an error to be set")
    fun assertNoError()   = assertNull(state.error,    "Expected no error")

    fun assertHasReachedEnd()  = assertTrue(state.hasReachedEnd,  "Expected hasReachedEnd=true")
    fun assertNotReachedEnd()  = assertFalse(state.hasReachedEnd, "Expected hasReachedEnd=false")
}
