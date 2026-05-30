package com.tekmoon.analytics

/**
 * Fans every event out to multiple [AnalyticsClient]s (e.g. Firebase + Mixpanel concurrently).
 *
 * Each delegate's call is wrapped in `runCatching` so one adapter throwing does not prevent the
 * others from receiving the event. Exceptions are swallowed — adapters are expected to handle
 * their own retry / logging internally.
 */
class MultiAnalyticsClient(
    private val clients: List<AnalyticsClient>,
) : AnalyticsClient {

    override fun track(event: String, params: Map<String, Any?>) {
        clients.forEach { runCatching { it.track(event, params) } }
    }

    override fun screen(name: String, params: Map<String, Any?>) {
        clients.forEach { runCatching { it.screen(name, params) } }
    }

    override fun identify(userId: String?, traits: Map<String, Any?>) {
        clients.forEach { runCatching { it.identify(userId, traits) } }
    }

    override fun reset() {
        clients.forEach { runCatching { it.reset() } }
    }

    override suspend fun flush() {
        clients.forEach { runCatching { it.flush() } }
    }
}
