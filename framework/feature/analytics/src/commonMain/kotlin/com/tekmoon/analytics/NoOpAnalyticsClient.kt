package com.tekmoon.analytics

/**
 * The default [AnalyticsClient] when no adapter has been configured. Every method is a no-op —
 * callers can `track(...)` freely without checking whether analytics is wired.
 *
 * Used as:
 * - the default in `LocalAnalytics` (designsystem CompositionLocal) so composables never need
 *   null-checks;
 * - the result of `Framework.analytics` when `FrameworkInit.analyticsClient` is `null`.
 */
object NoOpAnalyticsClient : AnalyticsClient {
    override fun track(event: String, params: Map<String, Any?>) = Unit
    override fun screen(name: String, params: Map<String, Any?>) = Unit
    override fun identify(userId: String?, traits: Map<String, Any?>) = Unit
    override fun reset() = Unit
    override suspend fun flush() = Unit
}
