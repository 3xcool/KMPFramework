package com.tekmoon.analytics

/**
 * Wrapper that explicitly marks an analytics property as personally identifiable.
 *
 * Plain `Map<String, Any?>` values are treated as anonymous and pass through to adapters
 * untouched. Values wrapped with [Pii] are routed through the installed [PiiPolicy] before
 * being forwarded — the policy decides whether to keep, transform, or drop them.
 *
 * Example:
 * ```kotlin
 * analytics.track("checkout_completed", mapOf(
 *     "amount"  to 99.50,                                    // anonymous, plain
 *     "cart_id" to cart.id,                                  // anonymous, plain
 *     "email"   to Pii(user.email, PiiClass.Personal),       // tagged Personal
 *     "ip"      to Pii(deviceIp, PiiClass.PseudoAnonymous),  // tagged pseudo
 * ))
 * ```
 *
 * Designed to be SDUI-serializable — a future server-config layer can introspect the
 * classification without bespoke handling.
 */
data class Pii(
    val value: Any?,
    val classification: PiiClass = PiiClass.Personal,
)

/**
 * LGPD-aware tiering for personally identifiable values.
 *
 * Mirrors the distinctions in **Lei nº 13.709 / 2018** (LGPD):
 * - Article 5, II — _dado pessoal_ (personal data) covers [Personal] and [PseudoAnonymous].
 * - Article 5, II + Article 11 — _dado pessoal sensível_ (sensitive personal data) is [Sensitive].
 *
 * Anonymous (non-PII) values are intentionally absent — they don't need a tag, they're just
 * raw entries in the params map.
 */
enum class PiiClass {
    /**
     * Indirect identifier (device ID, hashed user ID, session token). Still personal data under
     * LGPD but with a lower re-identification risk.
     */
    PseudoAnonymous,

    /**
     * Directly identifying information (email, full name, phone, address). LGPD Art. 5, II
     * core "dado pessoal" tier.
     */
    Personal,

    /**
     * Sensitive personal data per LGPD Art. 11: racial / ethnic origin, religious conviction,
     * political opinion, health or sexual life, genetic or biometric data, union affiliation.
     * Requires explicit consent and additional safeguards — most policies drop this tier.
     */
    Sensitive,
}
