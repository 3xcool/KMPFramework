package com.tekmoon.analytics

/**
 * Decorator that applies [policy] to every [Pii]-tagged property in incoming events before
 * delegating to [delegate]. Anonymous (untagged) properties pass through untouched.
 *
 * Installed automatically by `Framework.start`, so adapters (Firebase, Mixpanel, etc.) never
 * receive raw [Pii] values — they receive either the policy-transformed plain value or nothing
 * at all if the policy returned `null`.
 *
 * Also strips `identify(userId, ...)`'s [userId] when wrapped in [Pii] — wrap with
 * `Pii(userId, PseudoAnonymous)` if your provider should still pseudonymize it.
 */
class PolicyAnalyticsClient(
    private val delegate: AnalyticsClient,
    private val policy: PiiPolicy,
) : AnalyticsClient {

    override fun track(event: String, params: Map<String, Any?>) {
        delegate.track(event, applyPolicy(params))
    }

    override fun screen(name: String, params: Map<String, Any?>) {
        delegate.screen(name, applyPolicy(params))
    }

    override fun identify(userId: String?, traits: Map<String, Any?>) {
        delegate.identify(userId, applyPolicy(traits))
    }

    override fun reset() = delegate.reset()
    override suspend fun flush() = delegate.flush()

    private fun applyPolicy(params: Map<String, Any?>): Map<String, Any?> {
        if (params.isEmpty()) return params
        // Fast path: if there are no Pii values, skip the rebuild.
        if (params.values.none { it is Pii }) return params

        val out = LinkedHashMap<String, Any?>(params.size)
        for ((key, value) in params) {
            if (value is Pii) {
                val transformed = policy.transform(key, value)
                if (transformed != null) out[key] = transformed
                // null result = drop the key entirely
            } else {
                out[key] = value
            }
        }
        return out
    }
}
