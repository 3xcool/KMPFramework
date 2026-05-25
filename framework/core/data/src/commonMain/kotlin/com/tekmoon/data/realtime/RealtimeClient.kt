package com.tekmoon.data.realtime

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * A typed, reconnect-aware WebSocket client.
 *
 * ### Lifecycle
 * 1. Create via [RealtimeClient] factory or dependency injection.
 * 2. Call [connect] with a [CoroutineScope] (e.g. `viewModelScope`). The connection
 *    loop runs as a child of that scope — cancelling the scope tears it down automatically.
 * 3. Collect [events] for incoming messages and [connectionState] for UI feedback.
 * 4. Call [send] to push typed outgoing frames.
 * 5. Call [disconnect] for a clean close before the scope ends (optional but recommended).
 *
 * ### Message format
 * Frames are JSON with a discriminator key (default `"type"`):
 * ```json
 * { "type": "chat_message", "text": "hello" }
 * ```
 *
 * Your sealed event class needs `@Serializable` + `@SerialName` on every subclass:
 * ```kotlin
 * @Serializable
 * sealed interface AppEvent {
 *     @Serializable @SerialName("chat_message")
 *     data class ChatMessage(val text: String) : AppEvent
 *
 *     @Serializable @SerialName("user_joined")
 *     data class UserJoined(val userId: String) : AppEvent
 * }
 * ```
 *
 * ### ViewModel usage
 * ```kotlin
 * class ChatViewModel(httpClient: HttpClient, tokenProvider: TokenProvider) : ViewModel() {
 *
 *     private val client = RealtimeClient(
 *         httpClient = httpClient,
 *         config = RealtimeConfig(
 *             url = "wss://api.example.com/ws",
 *             serializer = AppEvent.serializer(),
 *             tokenProvider = tokenProvider,               // optional — omit for public endpoints
 *             reconnectPolicy = ReconnectPolicy.ExponentialBackoff(maxAttempts = 10),
 *         ),
 *     )
 *
 *     // Expose connection state to the UI (e.g. to show a "Reconnecting…" banner)
 *     val connectionState = client.connectionState
 *
 *     init {
 *         client.connect(viewModelScope)
 *         viewModelScope.launch {
 *             client.events.collect { event ->
 *                 when (event) {
 *                     is AppEvent.ChatMessage -> handleMessage(event)
 *                     is AppEvent.UserJoined  -> handleUserJoined(event)
 *                 }
 *             }
 *         }
 *     }
 *
 *     fun sendMessage(text: String) {
 *         viewModelScope.launch { client.send(AppEvent.ChatMessage(text)) }
 *     }
 *
 *     override fun onCleared() {
 *         viewModelScope.launch { client.disconnect() }
 *     }
 * }
 * ```
 *
 * @param Event The sealed event type shared by both incoming and outgoing frames.
 */
interface RealtimeClient<Event> {

    /** Current connection lifecycle state. Always up-to-date; never null. */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Hot stream of decoded incoming events.
     *
     * Frames that fail to decode are silently dropped.
     * Buffer capacity is controlled by [RealtimeConfig.incomingBufferCapacity].
     */
    val events: SharedFlow<Event>

    /**
     * Starts the connection loop inside [scope].
     *
     * Returns immediately; the handshake happens asynchronously. Calling [connect]
     * while already connected is a no-op.
     *
     * The loop respects [RealtimeConfig.reconnectPolicy]: on unexpected disconnect
     * it waits and retries. On clean [disconnect] or policy exhaustion it stops.
     *
     * @param scope Coroutine scope that owns the connection lifetime.
     */
    fun connect(scope: CoroutineScope)

    /**
     * Closes the WebSocket cleanly and stops the reconnect loop.
     *
     * Safe to call from any coroutine. Suspends until the connection is fully closed.
     */
    suspend fun disconnect()

    /**
     * Encodes [event] as JSON and sends it as a text frame.
     *
     * @throws IllegalStateException if not currently [ConnectionState.Connected].
     */
    suspend fun send(event: Event)

    /**
     * Sends a raw text frame without serialization — escape hatch for legacy payloads
     * or frames that don't fit the typed model.
     *
     * @throws IllegalStateException if not currently [ConnectionState.Connected].
     */
    suspend fun sendRaw(frame: String)

    companion object {
        /**
         * Creates a [RealtimeClient] backed by Ktor's WebSocket engine.
         *
         * The [httpClient] must have the WebSockets plugin installed — guaranteed when
         * created via [com.tekmoon.data.networking.HttpClientFactory].
         *
         * ```kotlin
         * val client = RealtimeClient(httpClient, config)
         * client.connect(viewModelScope)
         * ```
         */
        operator fun <Event> invoke(
            httpClient: HttpClient,
            config: RealtimeConfig<Event>,
        ): RealtimeClient<Event> = RealtimeClientImpl(httpClient, config)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Implementation
// ─────────────────────────────────────────────────────────────────────────────

internal class RealtimeClientImpl<Event>(
    private val httpClient: HttpClient,
    private val config: RealtimeConfig<Event>,
) : RealtimeClient<Event> {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = config.incomingBufferCapacity,
    )
    override val events: SharedFlow<Event> = _events.asSharedFlow()

    @Volatile private var session: DefaultClientWebSocketSession? = null
    private var connectJob: Job? = null

    /** Set by [disconnect] to break the reconnect loop without cancelling the caller's scope. */
    @Volatile private var intentionalDisconnect = false

    // ── Public API ────────────────────────────────────────────────────────────

    override fun connect(scope: CoroutineScope) {
        if (connectJob?.isActive == true) return
        intentionalDisconnect = false
        connectJob = scope.launch { runConnectionLoop() }
    }

    override suspend fun disconnect() {
        intentionalDisconnect = true
        session?.close()
        connectJob?.cancelAndJoin()
        connectJob = null
        _connectionState.value = ConnectionState.Disconnected()
    }

    override suspend fun send(event: Event) {
        sendRaw(config.json.encodeToString(config.serializer, event))
    }

    override suspend fun sendRaw(frame: String) {
        val s = session ?: error("RealtimeClient is not connected — call connect() first.")
        s.send(Frame.Text(frame))
    }

    // ── Connection loop ───────────────────────────────────────────────────────

    private suspend fun runConnectionLoop() {
        var attempt = 0

        while (!intentionalDisconnect) {
            _connectionState.value = ConnectionState.Connecting

            val established = tryConnect()
            if (!established || intentionalDisconnect) break

            // Unexpected drop — consult reconnect policy.
            attempt++
            val delayMs = nextDelayMs(attempt) ?: break   // null = give up

            _connectionState.value = ConnectionState.Reconnecting(
                attempt = attempt,
                delayMs = delayMs,
            )
            delay(delayMs)
        }

        _connectionState.value = ConnectionState.Disconnected()
    }

    /**
     * Opens the WebSocket and drains frames until the connection closes.
     *
     * @return `true` if the handshake succeeded, `false` on initial failure.
     */
    private suspend fun tryConnect(): Boolean {
        return try {
            // webSocket(urlString, request, block) sets the URL first, then runs the
            // request block — so query-param token delivery appends cleanly.
            httpClient.webSocket(
                urlString = config.url,
                request = {
                    config.tokenProvider?.getToken()?.let { token ->
                        config.tokenDelivery.applyToken(token, this)
                    }
                },
            ) {
                session = this
                _connectionState.value = ConnectionState.Connected

                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> decodeAndEmit(frame.readText())
                            is Frame.Close -> break
                            else -> Unit  // Ping / Pong handled by Ktor engine
                        }
                        if (intentionalDisconnect) break
                    }
                } catch (_: ClosedReceiveChannelException) {
                    // Normal: server closed the channel.
                } finally {
                    session = null
                }
            }
            true
        } catch (_: Exception) {
            session = null
            false
        }
    }

    private fun decodeAndEmit(text: String) {
        try {
            val event = config.json.decodeFromString(config.serializer, text)
            _events.tryEmit(event)
        } catch (_: Exception) {
            // Decode failure — frame dropped.
            // TODO: route to ShowMeLoggerK once Framework.start() guarantees a logger.
        }
    }

    /**
     * Returns the delay in milliseconds before reconnect attempt [attempt],
     * or `null` to stop retrying.
     *
     * Suspend so that [ReconnectPolicy.Custom.nextDelay] (itself a suspend lambda)
     * can be called directly here without requiring a separate coroutine.
     */
    private suspend fun nextDelayMs(attempt: Int): Long? {
        return when (val policy = config.reconnectPolicy) {
            ReconnectPolicy.Never -> null

            is ReconnectPolicy.Fixed -> {
                if (attempt > policy.maxAttempts) null
                else policy.delay.inWholeMilliseconds
            }

            is ReconnectPolicy.ExponentialBackoff -> {
                if (attempt > policy.maxAttempts) return null
                val base = policy.initialDelay.inWholeMilliseconds *
                        policy.multiplier.pow((attempt - 1).toDouble())
                val capped = min(base, policy.maxDelay.inWholeMilliseconds.toDouble()).toLong()
                if (policy.jitter) (capped * (0.5 + Random.nextDouble() * 0.5)).toLong()
                else capped
            }

            is ReconnectPolicy.Custom -> {
                policy.nextDelay(attempt)?.inWholeMilliseconds
            }
        }
    }
}
