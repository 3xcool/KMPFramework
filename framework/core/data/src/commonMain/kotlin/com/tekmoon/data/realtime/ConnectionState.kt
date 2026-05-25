package com.tekmoon.data.realtime

/**
 * Lifecycle states of a [RealtimeClient] connection.
 *
 * Consumers can collect [RealtimeClient.connectionState] to drive UI banners,
 * retry buttons, or offline indicators.
 */
sealed interface ConnectionState {

    /** First connect attempt is in flight. */
    data object Connecting : ConnectionState

    /** WebSocket handshake completed; frames are flowing. */
    data object Connected : ConnectionState

    /**
     * A disconnect occurred and the client is waiting before the next attempt.
     *
     * @param attempt   1-based reconnect attempt number.
     * @param delayMs   How long the client will wait before retrying (milliseconds).
     */
    data class Reconnecting(
        val attempt: Int,
        val delayMs: Long,
    ) : ConnectionState

    /**
     * The client is not connected and will not try again.
     *
     * This is the initial state and the terminal state after [RealtimeClient.disconnect]
     * or after the [ReconnectPolicy] is exhausted.
     *
     * @param cause The exception that triggered the disconnect, or `null` for a clean close.
     */
    data class Disconnected(
        val cause: Throwable? = null,
    ) : ConnectionState
}
