package com.tekmoon.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe coordinator for obtaining valid auth tokens in a concurrent environment.
 *
 * Any code that needs a valid token — an HTTP client, a WebSocket connection, a download
 * manager — can share a single [TokenGate] instance. The gate guarantees that when many
 * coroutines detect an expired token simultaneously, the refresh endpoint is called
 * **exactly once**.
 *
 * ### Usage
 * ```kotlin
 * val gate = TokenGate(
 *     tokenProvider = myTokenProvider,
 *     onSessionExpired = { authEvents.emit(AuthEvent.SessionExpired) },
 * )
 *
 * // Fast path — current token, no locking
 * val token = gate.getToken()
 *
 * // On auth failure (e.g. 401) — single-flight refresh
 * val freshToken = gate.refresh() ?: return // session expired, handle logout
 * ```
 *
 * ### Thundering-herd protection
 * Suppose 10 concurrent requests all receive HTTP 401 at the same time:
 *
 * ```
 * Requests A…J each call gate.refresh() with token T1 in storage
 *
 * A  → captures tokenBeforeWait = T1
 * B…J → each capture tokenBeforeWait = T1
 *
 * A  → wins the mutex → tokenNow == T1 → calls refreshToken() → stores T2 → releases
 * B  → acquires mutex → tokenNow == T2 ≠ T1 → returns T2 immediately (no refresh call)
 * C…J → same as B
 * ```
 *
 * At most one call to [TokenProvider.refreshToken] is made per expiry cycle.
 *
 * @param tokenProvider Source of the current token and the refresh logic.
 * @param onSessionExpired Optional callback invoked when [TokenProvider.refreshToken] fails
 *   (e.g. emit [AuthEvent.SessionExpired] to route the user to the login screen).
 */
class TokenGate(
    private val tokenProvider: TokenProvider,
    private val onSessionExpired: (suspend () -> Unit)? = null,
) {
    private val mutex = Mutex()

    /**
     * Returns the current stored token without acquiring any lock.
     *
     * Use this for injecting the token before sending a request. If the token is `null`
     * (user not authenticated), the caller should skip auth or handle it appropriately.
     */
    suspend fun getToken(): String? = tokenProvider.getToken()

    /**
     * Acquires the refresh lock and returns a valid token.
     *
     * Internally, the gate snapshots the token **before** blocking on the mutex. After
     * acquiring the lock, it compares the snapshot to what is stored now:
     * - **Different** → another coroutine already refreshed while this one was waiting;
     *   the new token is returned immediately without an extra network call.
     * - **Same** → this coroutine is the first to reach the lock for this expiry cycle;
     *   [TokenProvider.refreshToken] is called exactly once.
     *
     * @return The refreshed access token, or `null` if [TokenProvider.refreshToken] failed
     *   (in which case [onSessionExpired] is also invoked).
     */
    suspend fun refresh(): String? {
        // Snapshot the token BEFORE waiting for the mutex.
        // This is what we consider "stale" — if it has changed by the time we hold the
        // lock, another coroutine already did the refresh.
        val tokenBeforeWait = tokenProvider.getToken()

        return mutex.withLock {
            val tokenNow = tokenProvider.getToken()

            if (tokenNow != null && tokenNow != tokenBeforeWait) {
                // A concurrent coroutine refreshed while we were queued at the mutex.
                // Reuse their result — no extra refresh call needed.
                tokenNow
            } else {
                // We are the first (or only) coroutine to detect this expiry cycle.
                when (val result = tokenProvider.refreshToken()) {
                    is com.tekmoon.domain.util.data.Result.Success -> result.data
                    is com.tekmoon.domain.util.data.Result.Failure -> {
                        onSessionExpired?.invoke()
                        null
                    }
                }
            }
        }
    }
}
