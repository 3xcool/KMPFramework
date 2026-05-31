package com.tekmoon.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiAnalyticsClientTest {

    @Test
    fun fans_track_out_to_every_delegate() {
        val a = RecordingAnalyticsClient()
        val b = RecordingAnalyticsClient()
        val multi = MultiAnalyticsClient(listOf(a, b))

        multi.track("event", mapOf("k" to 1))

        assertEquals(1, a.tracks.size)
        assertEquals(1, b.tracks.size)
        assertEquals("event", a.tracks.single().event)
        assertEquals("event", b.tracks.single().event)
    }

    @Test
    fun fans_screen_identify_reset_and_flush() = runTest {
        val a = RecordingAnalyticsClient()
        val b = RecordingAnalyticsClient()
        val multi = MultiAnalyticsClient(listOf(a, b))

        multi.screen("home")
        multi.identify("u", mapOf("plan" to "pro"))
        multi.reset()
        multi.flush()

        listOf(a, b).forEach { client ->
            assertEquals(listOf("home"), client.screens.map { it.name })
            assertEquals(listOf("u"), client.identifies.map { it.userId })
            // Reset + Flush calls show up once each
            assertEquals(1, client.calls.count { it is RecordingAnalyticsClient.Call.Reset })
            assertEquals(1, client.calls.count { it is RecordingAnalyticsClient.Call.Flush })
        }
    }

    @Test
    fun one_delegate_throwing_does_not_prevent_the_others() {
        val good = RecordingAnalyticsClient()
        val bad = ThrowingAnalyticsClient()
        val alsoGood = RecordingAnalyticsClient()
        val multi = MultiAnalyticsClient(listOf(good, bad, alsoGood))

        // Must not throw.
        multi.track("event")

        assertEquals(1, good.tracks.size)
        assertEquals(1, alsoGood.tracks.size)
    }
}

/** Throws on every interaction — used to verify MultiAnalyticsClient swallows failures. */
private class ThrowingAnalyticsClient : AnalyticsClient {
    override fun track(event: String, params: Map<String, Any?>): Unit = error("bad track")
    override fun screen(name: String, params: Map<String, Any?>): Unit = error("bad screen")
    override fun identify(userId: String?, traits: Map<String, Any?>): Unit = error("bad identify")
    override fun reset(): Unit = error("bad reset")
    override suspend fun flush(): Unit = error("bad flush")
}
