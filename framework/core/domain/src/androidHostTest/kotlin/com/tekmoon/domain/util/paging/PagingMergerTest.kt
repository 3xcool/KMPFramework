package com.tekmoon.domain.util.paging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for [PagingMerger].
 *
 * All tests are synchronous — no coroutines needed.
 */
class PagingMergerTest {

    private fun merger(
        pageSize: Int = 3,
        resolveConflict: ((local: TestItem, remote: TestItem) -> TestItem)? = null,
    ) = PagingMerger(
        PagingConfig(
            pageSize = pageSize,
            extractKey = { it.id },
            getTimestamp = { it.updatedAt },
            resolveConflict = resolveConflict,
        )
    )

    // -----------------------------------------------------------------------
    // Empty / trivial cases
    // -----------------------------------------------------------------------

    @Test
    fun merge_emptyExisting_emptyIncoming_returnsEmpty() {
        val result = merger().merge(emptyList(), emptyList(), PagingItemSource.Remote)
        assertEquals(0, result.size)
    }

    @Test
    fun merge_emptyExisting_incomingItems_returnsIncomingAsRemote() {
        val incoming = listOf(TestItem("a"), TestItem("b"))
        val result = merger().merge(emptyList(), incoming, PagingItemSource.Remote)

        assertEquals(2, result.size)
        assertEquals("a", result[0].data.id)
        assertEquals(PagingItemSource.Remote, result[0].source)
        assertEquals("b", result[1].data.id)
        assertEquals(PagingItemSource.Remote, result[1].source)
    }

    @Test
    fun merge_existingItems_emptyIncoming_preservesExisting() {
        val existing = listOf(
            PagingItem(TestItem("a"), PagingItemSource.Local),
            PagingItem(TestItem("b"), PagingItemSource.Remote),
        )
        val result = merger().merge(existing, emptyList(), PagingItemSource.Remote)

        assertEquals(2, result.size)
        assertEquals("a", result[0].data.id)
        assertEquals("b", result[1].data.id)
    }

    // -----------------------------------------------------------------------
    // Deduplication
    // -----------------------------------------------------------------------

    @Test
    fun merge_noOverlap_appendsAllIncomingAfterExisting() {
        val existing = listOf(PagingItem(TestItem("a"), PagingItemSource.Remote))
        val incoming = listOf(TestItem("b"), TestItem("c"))

        val result = merger().merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(3, result.size)
        assertEquals(listOf("a", "b", "c"), result.map { it.data.id })
    }

    @Test
    fun merge_duplicateKeyInIncoming_lastOccurrenceWins() {
        val incoming = listOf(
            TestItem("a", name = "first",  updatedAt = 1L),
            TestItem("a", name = "second", updatedAt = 2L),
        )
        val result = merger().merge(emptyList(), incoming, PagingItemSource.Remote)

        assertEquals(1, result.size)
        assertEquals("second", result[0].data.name)
    }

    // -----------------------------------------------------------------------
    // Timestamp-based conflict resolution (no custom resolver)
    // -----------------------------------------------------------------------

    @Test
    fun merge_remoteNewer_remoteWins() {
        val existing = listOf(PagingItem(TestItem("a", name = "old", updatedAt = 1L), PagingItemSource.Local))
        val incoming = listOf(TestItem("a", name = "new", updatedAt = 2L))

        val result = merger().merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(1, result.size)
        assertEquals("new", result[0].data.name)
        assertEquals(PagingItemSource.Remote, result[0].source)
    }

    @Test
    fun merge_existingNewer_existingWins() {
        val existing = listOf(PagingItem(TestItem("a", name = "local-edit", updatedAt = 5L), PagingItemSource.Local))
        val incoming = listOf(TestItem("a", name = "server", updatedAt = 2L))

        val result = merger().merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(1, result.size)
        assertEquals("local-edit", result[0].data.name)
        assertEquals(PagingItemSource.Local, result[0].source)
    }

    @Test
    fun merge_tieTimestamp_existingPreserved() {
        val existing = listOf(PagingItem(TestItem("a", name = "local", updatedAt = 3L), PagingItemSource.Local))
        val incoming = listOf(TestItem("a", name = "remote", updatedAt = 3L))

        val result = merger().merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(1, result.size)
        assertEquals("local", result[0].data.name) // existing preserved on tie
        assertEquals(PagingItemSource.Local, result[0].source)
    }

    @Test
    fun merge_itemsOnlyInExisting_alwaysPreserved() {
        val existing = listOf(
            PagingItem(TestItem("a", updatedAt = 1L), PagingItemSource.Local),
            PagingItem(TestItem("b", updatedAt = 1L), PagingItemSource.Local),
        )
        // Remote only knows about "b"
        val incoming = listOf(TestItem("b", updatedAt = 2L))

        val result = merger().merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(2, result.size)
        val ids = result.map { it.data.id }
        assert("a" in ids) { "Item 'a' (local-only) should be preserved" }
        assert("b" in ids) { "Item 'b' should still be present" }
    }

    // -----------------------------------------------------------------------
    // Custom resolveConflict
    // -----------------------------------------------------------------------

    @Test
    fun merge_customResolver_calledForLocalRemoteConflict() {
        var resolverCalled = false
        val m = merger(resolveConflict = { local, remote ->
            resolverCalled = true
            local // local always wins in this test
        })

        val existing = listOf(PagingItem(TestItem("a", name = "local"), PagingItemSource.Local))
        val incoming = listOf(TestItem("a", name = "remote"))

        m.merge(existing, incoming, PagingItemSource.Remote)
        assert(resolverCalled) { "resolveConflict should have been called for Local/Remote conflict" }
    }

    @Test
    fun merge_customResolver_localWins_sourceTaggedLocal() {
        val m = merger(resolveConflict = { local, _ -> local })

        val localItem = TestItem("a", name = "local-edit")
        val existing = listOf(PagingItem(localItem, PagingItemSource.Local))
        val incoming = listOf(TestItem("a", name = "remote"))

        val result = m.merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(PagingItemSource.Local, result[0].source)
        assertEquals("local-edit", result[0].data.name)
    }

    @Test
    fun merge_customResolver_remoteWins_sourceTaggedRemote() {
        val m = merger(resolveConflict = { _, remote -> remote })

        val existing = listOf(PagingItem(TestItem("a", name = "local"), PagingItemSource.Local))
        val remoteItem = TestItem("a", name = "server-update")
        val incoming = listOf(remoteItem)

        val result = m.merge(existing, incoming, PagingItemSource.Remote)

        assertEquals(PagingItemSource.Remote, result[0].source)
        assertEquals("server-update", result[0].data.name)
    }

    @Test
    fun merge_customResolver_notCalledForSameSourceConflict() {
        var resolverCalled = false
        val m = merger(resolveConflict = { local, remote ->
            resolverCalled = true
            remote
        })

        // Both are Remote — same-source conflict → falls back to timestamp
        val existing = listOf(PagingItem(TestItem("a", updatedAt = 1L), PagingItemSource.Remote))
        val incoming = listOf(TestItem("a", updatedAt = 2L))

        m.merge(existing, incoming, PagingItemSource.Remote)

        assert(!resolverCalled) { "resolveConflict must NOT be called for same-source (Remote/Remote) conflict" }
    }

    @Test
    fun merge_customResolver_receivesCorrectLocalAndRemoteArguments() {
        val capturedArgs = mutableListOf<Pair<TestItem, TestItem>>()
        val m = merger(resolveConflict = { local, remote ->
            capturedArgs.add(local to remote)
            local
        })

        val localItem = TestItem("a", name = "local")
        val remoteItem = TestItem("a", name = "remote")
        val existing = listOf(PagingItem(localItem, PagingItemSource.Local))

        m.merge(existing, listOf(remoteItem), PagingItemSource.Remote)

        assertEquals(1, capturedArgs.size)
        assertSame(localItem,  capturedArgs[0].first,  "first arg should be the LOCAL item")
        assertSame(remoteItem, capturedArgs[0].second, "second arg should be the REMOTE item")
    }

    // -----------------------------------------------------------------------
    // Source tags
    // -----------------------------------------------------------------------

    @Test
    fun merge_newItemsTaggedWithIncomingSource() {
        val result = merger().merge(
            existing = emptyList(),
            incoming = listOf(TestItem("a"), TestItem("b")),
            incomingSource = PagingItemSource.Local,
        )
        result.forEach {
            assertEquals(PagingItemSource.Local, it.source)
        }
    }

    @Test
    fun merge_preservesSourceOfUnchangedExistingItems() {
        val existing = listOf(
            PagingItem(TestItem("a"), PagingItemSource.Remote),
            PagingItem(TestItem("b"), PagingItemSource.Local),
        )
        // Incoming only has "c" — "a" and "b" should keep their original source tags.
        val result = merger().merge(existing, listOf(TestItem("c")), PagingItemSource.Remote)

        val byId = result.associateBy { it.data.id }
        assertEquals(PagingItemSource.Remote, byId["a"]?.source)
        assertEquals(PagingItemSource.Local,  byId["b"]?.source)
        assertEquals(PagingItemSource.Remote, byId["c"]?.source)
    }
}
