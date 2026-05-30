package com.tekmoon.analytics

/**
 * In-memory [AnalyticsClient] that records every call so tests can assert against them.
 *
 * Lives in commonMain (not commonTest) so downstream consumers can use it in their own tests
 * without depending on test artifacts. Thread-safe enough for sequential test use; not designed
 * for concurrent recording.
 */
class RecordingAnalyticsClient : AnalyticsClient {

    sealed interface Call {
        data class Track(val event: String, val params: Map<String, Any?>) : Call
        data class Screen(val name: String, val params: Map<String, Any?>) : Call
        data class Identify(val userId: String?, val traits: Map<String, Any?>) : Call
        data object Reset : Call
        data object Flush : Call
    }

    private val _calls = mutableListOf<Call>()
    val calls: List<Call> get() = _calls.toList()

    /** All [Call.Track] entries in the order they were recorded. */
    val tracks: List<Call.Track> get() = _calls.filterIsInstance<Call.Track>()

    /** All [Call.Screen] entries in the order they were recorded. */
    val screens: List<Call.Screen> get() = _calls.filterIsInstance<Call.Screen>()

    /** All [Call.Identify] entries in the order they were recorded. */
    val identifies: List<Call.Identify> get() = _calls.filterIsInstance<Call.Identify>()

    fun clear() {
        _calls.clear()
    }

    override fun track(event: String, params: Map<String, Any?>) {
        _calls += Call.Track(event, params)
    }

    override fun screen(name: String, params: Map<String, Any?>) {
        _calls += Call.Screen(name, params)
    }

    override fun identify(userId: String?, traits: Map<String, Any?>) {
        _calls += Call.Identify(userId, traits)
    }

    override fun reset() {
        _calls += Call.Reset
    }

    override suspend fun flush() {
        _calls += Call.Flush
    }
}
