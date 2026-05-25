package com.tekmoon.data.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [RealtimeClient] test double.
 *
 * Use in unit tests to push fake incoming events, verify outgoing sends, and
 * control connection state — no real WebSocket or Ktor engine involved.
 *
 * ### Example
 * ```kotlin
 * val fake = FakeRealtimeClient<AppEvent>()
 *
 * // Simulate incoming event from the server
 * fake.emitEvent(AppEvent.UserJoined("u1"))
 *
 * // Assert outgoing messages sent by the ViewModel
 * assertEquals(AppEvent.SendMessage("hello"), fake.sentEvents.last())
 *
 * // Simulate a drop → reconnecting cycle
 * fake.simulateDrop()
 * assertEquals(ConnectionState.Reconnecting(1, 0), fake.connectionState.value)
 * ```
 *
 * @param Event The event type, same as the production [RealtimeClient].
 */
class FakeRealtimeClient<Event> : RealtimeClient<Event> {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    override val events: SharedFlow<Event> = _events.asSharedFlow()

    /** All events sent by the system under test via [send] or [sendRaw], in order. */
    val sentEvents: List<Event> get() = _sentEvents
    private val _sentEvents = mutableListOf<Event>()

    /** All raw frames sent via [sendRaw], in order. */
    val sentRawFrames: List<String> get() = _sentRawFrames
    private val _sentRawFrames = mutableListOf<String>()

    /** Whether [connect] has been called at least once. */
    var connectCalled: Boolean = false
        private set

    /** Whether [disconnect] has been called. */
    var disconnectCalled: Boolean = false
        private set

    // ── RealtimeClient API ────────────────────────────────────────────────────

    override fun connect(scope: CoroutineScope) {
        connectCalled = true
        _connectionState.value = ConnectionState.Connected
    }

    override suspend fun disconnect() {
        disconnectCalled = true
        _connectionState.value = ConnectionState.Disconnected()
    }

    override suspend fun send(event: Event) {
        _sentEvents.add(event)
    }

    override suspend fun sendRaw(frame: String) {
        _sentRawFrames.add(frame)
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /**
     * Pushes [event] into [events] as if it arrived from the server.
     */
    suspend fun emitEvent(event: Event) {
        _events.emit(event)
    }

    /**
     * Overrides [connectionState] directly. Use to simulate any state transition.
     *
     * ```kotlin
     * fake.setConnectionState(ConnectionState.Reconnecting(attempt = 1, delayMs = 2_000))
     * ```
     */
    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    /**
     * Simulates an unexpected server-side drop by transitioning to
     * [ConnectionState.Reconnecting] with attempt=1 and 0 ms delay.
     *
     * The state does NOT advance to [ConnectionState.Connected] automatically —
     * call [setConnectionState] or [simulateReconnect] to continue the simulation.
     */
    fun simulateDrop(cause: Throwable? = null) {
        _connectionState.value = ConnectionState.Reconnecting(attempt = 1, delayMs = 0L)
    }

    /**
     * Simulates a successful reconnect after [simulateDrop].
     * Transitions back to [ConnectionState.Connected].
     */
    fun simulateReconnect() {
        _connectionState.value = ConnectionState.Connected
    }

    /**
     * Resets all recorded state: sent events, raw frames, and flags.
     * Does NOT change [connectionState].
     */
    fun reset() {
        _sentEvents.clear()
        _sentRawFrames.clear()
        connectCalled = false
        disconnectCalled = false
    }
}
