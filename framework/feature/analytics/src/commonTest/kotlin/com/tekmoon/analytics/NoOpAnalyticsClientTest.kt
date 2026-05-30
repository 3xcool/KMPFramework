package com.tekmoon.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class NoOpAnalyticsClientTest {

    @Test
    fun every_method_is_silent() = runTest {
        // Smoke: invoking any combination must not throw and must produce no observable effect.
        NoOpAnalyticsClient.track("event", mapOf("x" to 1))
        NoOpAnalyticsClient.screen("home")
        NoOpAnalyticsClient.identify("user", mapOf("plan" to "pro"))
        NoOpAnalyticsClient.reset()
        NoOpAnalyticsClient.flush()
    }
}
