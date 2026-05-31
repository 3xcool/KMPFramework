package com.tekmoon.backgroundwork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackgroundTaskRegistryTest {

    @Test
    fun register_then_lookup_returns_handler() = runTest {
        val registry = BackgroundTaskRegistry()
        val handler = BackgroundTaskHandler { BackgroundResult.Success }

        registry.register(StringTaskKind("sync-messages"), handler)

        assertNotNull(registry.handler("sync-messages"))
        assertEquals(handler, registry.handler("sync-messages"))
    }

    @Test
    fun lookup_for_unknown_kind_is_null() {
        val registry = BackgroundTaskRegistry()
        assertNull(registry.handler("never-registered"))
    }

    @Test
    fun unregister_removes_handler() = runTest {
        val registry = BackgroundTaskRegistry()
        val kind = StringTaskKind("kind-a")
        registry.register(kind, BackgroundTaskHandler { BackgroundResult.Success })
        registry.unregister(kind)
        assertNull(registry.handler(kind))
    }

    @Test
    fun kinds_returns_all_registered() = runTest {
        val registry = BackgroundTaskRegistry()
        registry.register(StringTaskKind("a"), BackgroundTaskHandler { BackgroundResult.Success })
        registry.register(StringTaskKind("b"), BackgroundTaskHandler { BackgroundResult.Success })
        val kinds = registry.kinds()
        assertTrue("a" in kinds)
        assertTrue("b" in kinds)
        assertEquals(2, kinds.size)
    }
}
