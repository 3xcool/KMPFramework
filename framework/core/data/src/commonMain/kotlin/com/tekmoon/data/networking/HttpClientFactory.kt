package com.tekmoon.data.networking

import com.tekmoon.data.FrameworkConfig
import com.tekmoon.data.auth.AuthEvent
import com.tekmoon.data.auth.NoAuthKey
import com.tekmoon.data.auth.TokenProvider
import com.tekmoon.data.logging.SimpleLogger
import com.tekmoon.domain.util.data.Result
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Creates and configures a Ktor [HttpClient] for the framework.
 *
 * ### Auth (optional)
 * Pass a [TokenProvider] to enable automatic Bearer token injection and single-flight
 * token refresh on 401 responses. Omitting it (or passing `null`) produces a plain client
 * with no auth logic — suitable for public APIs or when auth is handled externally.
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

        tokenProvider?.let { provider -> installAuthInterceptor(client, provider) }

        return client
    }

    // ------------------------------------------------------------------------------------------
    // Auth interceptor
    // ------------------------------------------------------------------------------------------

    /**
     * Installs a [HttpSend] interceptor that:
     * 1. Injects `Authorization: Bearer <token>` on every request (unless [NoAuthKey] is set).
     * 2. On HTTP 401, acquires [refreshMutex] and calls [TokenProvider.refreshToken] — but only
     *    if the token has not already been refreshed by a concurrent request (single-flight).
     * 3. Retries the original request with the new token on success.
     * 4. Emits [AuthEvent.SessionExpired] and returns the 401 response on failure.
     *
     * ### Thundering-herd protection
     * When many requests are in-flight simultaneously and the token expires, all of them will
     * receive 401. They all race to acquire [refreshMutex]:
     * - The **winner** calls `refreshToken()` and stores the new token inside [TokenProvider].
     * - Each **waiter** re-reads the token after acquiring the lock; if it has changed since the
     *   original request, no additional refresh call is made — the waiter retries immediately.
     *
     * This guarantees the refresh endpoint is called **at most once** per expiry cycle.
     */
    private fun installAuthInterceptor(client: HttpClient, provider: TokenProvider) {
        // One mutex per HttpClient instance — guards the refresh call.
        val refreshMutex = Mutex()

        client.plugin(HttpSend).intercept { request ->
            // Requests marked with noAuth() skip all auth logic.
            if (request.attributes.contains(NoAuthKey)) {
                return@intercept execute(request)
            }

            // ── Step 1: inject the current token ────────────────────────────────────────────
            val tokenAtSend = provider.getToken()
            if (tokenAtSend != null) {
                request.headers[HttpHeaders.Authorization] = "Bearer $tokenAtSend"
            }

            val originalCall = execute(request)

            // ── Step 2: only act on 401; skip if no token was sent (nothing to refresh) ────
            if (originalCall.response.status != HttpStatusCode.Unauthorized || tokenAtSend == null) {
                return@intercept originalCall
            }

            // ── Step 3: single-flight refresh ───────────────────────────────────────────────
            //
            // Acquire the mutex so exactly one coroutine calls refreshToken() at a time.
            // All concurrent callers that also received 401 wait here, then re-check whether
            // the token was already updated before deciding to refresh again.
            val refreshResult = refreshMutex.withLock {
                val tokenNow = provider.getToken()
                if (tokenNow != null && tokenNow != tokenAtSend) {
                    // Another coroutine already refreshed while we were waiting — reuse its token.
                    Result.Success(tokenNow)
                } else {
                    // We are the first (or only) caller — perform the actual refresh.
                    provider.refreshToken()
                }
            }

            // ── Step 4: retry or signal session expiry ──────────────────────────────────────
            when (refreshResult) {
                is Result.Success -> {
                    // Replace the stale token and re-send the original request.
                    request.headers[HttpHeaders.Authorization] = "Bearer ${refreshResult.data}"
                    execute(request)
                }
                is Result.Failure -> {
                    // Refresh token is expired / revoked → the session is over.
                    // Notify observers (e.g. the nav host) and surface the 401 to the caller.
                    _authEvents.tryEmit(AuthEvent.SessionExpired)
                    originalCall
                }
            }
        }
    }
}
