package com.tekmoon.analytics

/**
 * Framework-side abstraction for product analytics.
 *
 * Implementations forward events to a backend SDK (Firebase / Mixpanel / Amplitude / etc.).
 * The framework's own contributions are:
 *
 * - **Vendor-neutral contract** — call sites depend on this interface, not on a vendor SDK, so
 *   you can swap analytics providers without touching feature code.
 * - **PII guard** — see [Pii], [PiiClass], [PiiPolicy]. Call sites tag sensitive values; the
 *   [PolicyAnalyticsClient] decorator (installed by `Framework.start`) enforces the configured
 *   [PiiPolicy] before any value reaches the adapter.
 *
 * ## Access pattern
 *
 * `Framework.start` constructs the policy-wrapped client and exposes it via `Framework.analytics`.
 * Inject it via constructors (preferred) — global access via the singleton is intentionally not
 * provided so call sites stay explicit and trivially testable with [RecordingAnalyticsClient].
 *
 * Composables read it from `LocalAnalytics` (provided in `:framework:core:designsystem`); the
 * interactive `Ds*` primitives forward `analyticsId` / `analyticsParams` to this client when set.
 */
interface AnalyticsClient {

    /** Records a generic event. */
    fun track(event: String, params: Map<String, Any?> = emptyMap())

    /** Records a screen-view event. Distinct from [track] because most adapters treat it specially. */
    fun screen(name: String, params: Map<String, Any?> = emptyMap())

    /**
     * Associates subsequent events with [userId]. Pass `null` to clear identification without
     * dropping cached traits. Implementations should treat [userId] itself as PII — wrap it
     * with [Pii] if your provider doesn't already pseudonymize.
     */
    fun identify(userId: String?, traits: Map<String, Any?> = emptyMap())

    /** Clears all cached identity + traits. Typical on user sign-out. */
    fun reset()

    /** Best-effort flush of pending in-flight events. Call before app shutdown if it matters. */
    suspend fun flush()
}
