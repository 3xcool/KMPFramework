package com.tekmoon.data.realtime

import com.tekmoon.data.auth.TokenProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Full configuration for a [RealtimeClient].
 *
 * Only [url] and [serializer] are required. Every other field has a sensible default
 * that can be overridden per-client.
 *
 * ### Minimal usage
 * ```kotlin
 * val config = RealtimeConfig(
 *     url = "wss://api.example.com/ws",
 *     serializer = AppEvent.serializer(),
 * )
 * ```
 *
 * ### Authenticated, custom back-off
 * ```kotlin
 * val config = RealtimeConfig(
 *     url = "wss://api.example.com/ws",
 *     serializer = AppEvent.serializer(),
 *     tokenProvider = myTokenProvider,
 *     tokenDelivery = TokenDelivery.QueryParam("access_token"),
 *     reconnectPolicy = ReconnectPolicy.ExponentialBackoff(
 *         initialDelay = 2.seconds,
 *         maxAttempts = 10,
 *     ),
 * )
 * ```
 *
 * @param Event         The sealed event type. Must be annotated with `@Serializable`
 *                      and each subclass with `@SerialName("<type-value>")`.
 * @param url           WebSocket endpoint (`wss://` or `ws://`).
 * @param serializer    kotlinx.serialization [KSerializer] for [Event].
 *                      Obtain via `MyEvent.serializer()` or `serializer<MyEvent>()`.
 * @param json          [Json] instance used for encode/decode. Customise the
 *                      `classDiscriminator` here if your backend uses a key other
 *                      than `"type"`. Defaults to lenient JSON ignoring unknown keys.
 * @param tokenProvider Optional [TokenProvider]. When non-null and [TokenProvider.getToken]
 *                      returns a non-null token, it is delivered via [tokenDelivery].
 * @param tokenDelivery How the token is attached to the upgrade request.
 *                      Ignored when [tokenProvider] is null. Defaults to query param `"token"`.
 * @param reconnectPolicy How the client behaves after a disconnection.
 *                        Defaults to [ReconnectPolicy.ExponentialBackoff] with infinite retries.
 * @param pingIntervalMs  Interval in milliseconds between keep-alive pings sent by Ktor.
 *                        Set to `null` to disable application-level pings (transport-level
 *                        pings from the engine may still occur). Defaults to 20 000 ms.
 * @param incomingBufferCapacity Number of incoming events buffered before back-pressure
 *                               is applied. Excess events are dropped (oldest first).
 *                               Defaults to 64.
 */
data class RealtimeConfig<Event>(
    val url: String,
    val serializer: KSerializer<Event>,
    val json: Json = DefaultRealtimeJson,
    val tokenProvider: TokenProvider? = null,
    val tokenDelivery: TokenDelivery = TokenDelivery.QueryParam(),
    val reconnectPolicy: ReconnectPolicy = ReconnectPolicy.ExponentialBackoff(),
    val pingIntervalMs: Long? = 20_000L,
    val incomingBufferCapacity: Int = 64,
)

/**
 * Default [Json] for WebSocket frame encoding/decoding.
 *
 * - `classDiscriminator = "type"` — matches the standard `{ "type": "...", ... }` envelope.
 * - `ignoreUnknownKeys = true` — safe across API versions.
 * - `isLenient = true` — tolerates minor server formatting quirks.
 */
val DefaultRealtimeJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    isLenient = true
}
