package com.tekmoon.data.networking

import com.tekmoon.data.FrameworkConfig
import com.tekmoon.data.auth.AuthEvent
import com.tekmoon.data.auth.NoAuthKey
import com.tekmoon.data.auth.TokenGate
import com.tekmoon.data.auth.TokenProvider
import com.tekmoon.data.logging.SimpleLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

/**
 * Creates and configures a Ktor [HttpClient] for the framework.
 *
 * ### Auth (optional)
 * Pass a [TokenProvider] to enable automatic Bearer token injection and single-flight
 * token refresh on 401 responses via [TokenGate]. Omitting it (or passing `null`) produces
 * a plain client with no auth logic — suitable for public APIs.
 *
 * ### Session expiry
 * If auth is enabled and [TokenProvider.refreshToken] fails, [authEvents] emits
 * [AuthEvent.SessionExpired]. Collect this in your root ViewModel / navigation host:
 * ```kotlin
 * httpClientFactory.authEvents.collect { event ->
 *     when (event) {
 *         AuthEvent.SessionExpired -> navController.navigate(LoginDestination)
 *     }
 * }
 * ```
 *
 * @param simpleLogger   Logger used by Ktor's [Logging] plugin.
 * @param tokenProvider  Optional auth token source. Supply one to enable the auth interceptor.
 */
class HttpClientFactory(
    val simpleLogger: SimpleLogger,
    private val tokenProvider: TokenProvider? = null,
) {
    private val _authEvents = MutableSharedFlow<AuthEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Emits [AuthEvent.SessionExpired] when token refresh fails.
     * Only active when a [TokenProvider] was supplied to the constructor.
     */
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    fun create(engine: HttpClientEngine): HttpClient {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        simpleLogger.debug(message)
                    }
                }
                level = LogLevel.ALL
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header("x-api-key", FrameworkConfig.apiKey)
                contentType(ContentType.Application.Json)
            }
        }

        tokenProvider?.let { provider ->
            val gate = TokenGate(
                tokenProvider = provider,
                onSessionExpired = { _authEvents.tryEmit(AuthEvent.SessionExpired) },
            )
            installAuthInterceptor(client, gate)
        }

        return client
    }

    // ------------------------------------------------------------------------------------------
    // Auth interceptor
    // ------------------------------------------------------------------------------------------

    /**
     * Installs a [HttpSend] interceptor wired to a [TokenGate].
     *
     * Three steps per request:
     * 1. **Inject** — read the current token from the gate and set `Authorization: Bearer`.
     * 2. **Execute** — send the request as normal.
     * 3. **Refresh & retry** (only on 401) — delegate to [TokenGate.refresh], which handles
     *    the single-flight mutex internally, then retry once with the new token.
     *
     * Requests marked with [NoAuthKey] (via `noAuth()`) bypass all three steps.
     */
    private fun installAuthInterceptor(client: HttpClient, gate: TokenGate) {
        client.plugin(HttpSend).intercept { request ->

            // ── Opt-out: public endpoints skip auth entirely ─────────────────────────────────
            if (request.attributes.contains(NoAuthKey)) {
                return@intercept execute(request)
            }

            // ── Step 1: inject current token ─────────────────────────────────────────────────
            val token = gate.getToken()
            if (token != null) {
                request.headers[HttpHeaders.Authorization] = "Bearer $token"
            }

            // ── Step 2: execute ───────────────────────────────────────────────────────────────
            val call = execute(request)

            // ── Step 3: on 401, single-flight refresh → retry ────────────────────────────────
            // Skip if there was no token to begin with — nothing to refresh.
            if (call.response.status != HttpStatusCode.Unauthorized || token == null) {
                return@intercept call
            }

            val freshToken = gate.refresh() ?: return@intercept call  // null = session expired

            request.headers[HttpHeaders.Authorization] = "Bearer $freshToken"
            execute(request)
        }
    }
}
