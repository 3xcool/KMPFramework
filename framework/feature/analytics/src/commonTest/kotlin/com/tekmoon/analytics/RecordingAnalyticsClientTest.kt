package com.tekmoon.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingAnalyticsClientTest {

    @Test
    fun records_every_call_in_order() {
        val client = RecordingAnalyticsClient()

        client.track("a", mapOf("x" to 1))
        client.screen("home")
        client.identify("user-1", mapOf("plan" to "pro"))
        client.reset()

        assertEquals(4, client.calls.size)
        assertEquals(
            RecordingAnalyticsClient.Call.Track("a", mapOf("x" to 1)),
            client.calls[0],
        )
        assertEquals(
            RecordingAnalyticsClient.Call.Screen("home", emptyMap()),
            client.calls[1],
        )
        assertEquals(
            RecordingAnalyticsClient.Call.Identify("user-1", mapOf("plan" to "pro")),
            client.calls[2],
        )
        assertEquals(RecordingAnalyticsClient.Call.Reset, client.calls[3])
    }

    @Test
    fun typed_filters_partition_calls_by_kind() {
        val client = RecordingAnalyticsClient()
        client.track("a")
        client.screen("home")
        client.track("b")

        assertEquals(listOf("a", "b"), client.tracks.map { it.event })
        assertEquals(listOf("home"), client.screens.map { it.name })
        assertTrue(client.identifies.isEmpty())
    }

    @Test
    fun flush_records_as_a_call() = runTest {
        val client = RecordingAnalyticsClient()
        client.flush()
        assertEquals(RecordingAnalyticsClient.Call.Flush, client.calls.single())
    }

    @Test
    fun clear_drops_history() {
        val client = RecordingAnalyticsClient()
        client.track("a")
        client.clear()
        assertTrue(client.calls.isEmpty())
    }
}
