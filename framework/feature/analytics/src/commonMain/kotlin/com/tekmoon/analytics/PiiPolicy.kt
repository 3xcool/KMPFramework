package com.tekmoon.analytics

/**
 * Decides what to do with each [Pii]-tagged property before it leaves the framework.
 *
 * Installed once at startup via `FrameworkInit.piiPolicy`. The default ([DropAll]) is
 * LGPD-safe — apps must consciously opt into a more permissive policy after collecting
 * lawful basis for processing (consent, contract, legitimate interest, etc.).
 *
 * Implementing a custom policy is straightforward — `transform` receives the property key
 * and the [Pii] wrapper; return the value to forward (any type), or `null` to drop the
 * property entirely.
 */
fun interface PiiPolicy {

    /**
     * @return the value to forward downstream, or `null` to remove this property from
     *   the event payload. The returned value should be a plain type the adapter SDK can
     *   serialize (string, number, boolean, etc.) — do not wrap it in [Pii] again.
     */
    fun transform(key: String, pii: Pii): Any?

    companion object {

        /**
         * Strips every [Pii]-tagged property regardless of classification. Anonymous (untagged)
         * properties pass through. **Default** — safest under LGPD before lawful basis is
         * established.
         */
        val DropAll: PiiPolicy = PiiPolicy { _, _ -> null }

        /**
         * Keeps [PiiClass.PseudoAnonymous] values; drops [PiiClass.Personal] and
         * [PiiClass.Sensitive]. Use when you have lawful basis for pseudonymized identifiers
         * (device ID, user UUID) but not directly identifying data.
         */
        val KeepPseudonymized: PiiPolicy = PiiPolicy { _, pii ->
            if (pii.classification == PiiClass.PseudoAnonymous) pii.value else null
        }

        /**
         * Forwards every value as-is, regardless of tier. **Only use after collecting explicit,
         * informed consent from the data subject.** Useful for crash-reporting adapters where
         * the user has opted into detailed diagnostics, or in development builds.
         */
        val PassThrough: PiiPolicy = PiiPolicy { _, pii -> pii.value }
    }
}
