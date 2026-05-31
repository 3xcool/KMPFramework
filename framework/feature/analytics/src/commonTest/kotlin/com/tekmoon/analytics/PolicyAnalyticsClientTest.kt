package com.tekmoon.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyAnalyticsClientTest {

    // ---- DropAll (default LGPD-safe policy) -------------------------------

    @Test
    fun dropAll_strips_every_pii_value_regardless_of_class() {
        val recorder = RecordingAnalyticsClient()
        val policy = PolicyAnalyticsClient(recorder, PiiPolicy.DropAll)

        policy.track(
            event = "checkout",
            params = mapOf(
                "amount" to 99.50,                                  // anonymous, plain
                "email" to Pii("u@x.com", PiiClass.Personal),
                "deviceId" to Pii("abc-123", PiiClass.PseudoAnonymous),
                "health" to Pii("diabetic", PiiClass.Sensitive),
            ),
        )

        val recorded = recorder.tracks.single().params
        // Anonymous passes through.
        assertEquals(99.50, recorded["amount"])
        // PII keys are stripped.
        assertFalse(recorded.containsKey("email"))
        assertFalse(recorded.containsKey("deviceId"))
        assertFalse(recorded.containsKey("health"))
    }

    // ---- KeepPseudonymized ------------------------------------------------

    @Test
    fun keepPseudonymized_keeps_pseudo_drops_personal_and_sensitive() {
        val recorder = RecordingAnalyticsClient()
        val policy = PolicyAnalyticsClient(recorder, PiiPolicy.KeepPseudonymized)

        policy.track(
            event = "session_start",
            params = mapOf(
                "deviceId" to Pii("abc-123", PiiClass.PseudoAnonymous),
                "email" to Pii("u@x.com", PiiClass.Personal),
                "ethnicity" to Pii("redacted", PiiClass.Sensitive),
                "appVersion" to "1.2.3",
            ),
        )

        val recorded = recorder.tracks.single().params
        assertEquals("abc-123", recorded["deviceId"])
        assertEquals("1.2.3", recorded["appVersion"])
        assertFalse(recorded.containsKey("email"))
        assertFalse(recorded.containsKey("ethnicity"))
    }

    // ---- PassThrough ------------------------------------------------------

    @Test
    fun passThrough_forwards_every_tier_unchanged() {
        val recorder = RecordingAnalyticsClient()
        val policy = PolicyAnalyticsClient(recorder, PiiPolicy.PassThrough)

        policy.track(
            event = "x",
            params = mapOf(
                "email" to Pii("u@x.com", PiiClass.Personal),
                "health" to Pii("diabetic", PiiClass.Sensitive),
            ),
        )

        val recorded = recorder.tracks.single().params
        assertEquals("u@x.com", recorded["email"])
        assertEquals("diabetic", recorded["health"])
    }

    // ---- Identify path ----------------------------------------------------

    @Test
    fun identify_traits_run_through_the_policy() {
        val recorder = RecordingAnalyticsClient()
        val policy = PolicyAnalyticsClient(recorder, PiiPolicy.DropAll)

        policy.identify(
            userId = "user-1",
            traits = mapOf(
                "email" to Pii("u@x.com"),
                "plan" to "pro",
            ),
        )

        val recorded = recorder.identifies.single()
        // userId is passed unchanged — wrapping it is the caller's responsibility.
        assertEquals("user-1", recorded.userId)
        assertEquals("pro", recorded.traits["plan"])
        assertFalse(recorded.traits.containsKey("email"))
    }

    // ---- Fast path: no Pii at all -----------------------------------------

    @Test
    fun no_pii_payload_is_forwarded_as_the_same_map_instance() {
        val recorder = RecordingAnalyticsClient()
        val policy = PolicyAnalyticsClient(recorder, PiiPolicy.DropAll)

        val params = mapOf("a" to 1, "b" to "x", "c" to true)
        policy.track("event", params)

        // Same map reference — the fast path in applyPolicy avoids the LinkedHashMap rebuild.
        assertTrue(recorder.tracks.single().params === params)
    }

    // ---- Custom policy ----------------------------------------------------

    @Test
    fun custom_policy_can_transform_values() {
        val recorder = RecordingAnalyticsClient()
        // Toy policy: hash to a sentinel string instead of dropping.
        val hashing = PiiPolicy { _, pii -> "hash(${pii.value})" }
        val policy = PolicyAnalyticsClient(recorder, hashing)

        policy.track("x", mapOf("email" to Pii("u@x.com")))
        assertEquals("hash(u@x.com)", recorder.tracks.single().params["email"])
    }
}
