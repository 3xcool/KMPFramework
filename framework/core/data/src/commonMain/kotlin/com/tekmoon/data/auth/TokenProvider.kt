package com.tekmoon.data.auth

import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result

/**
 * Contract between the HTTP layer and wherever access tokens are stored.
 *
 * Implement this in your auth feature module (or the client app) and pass it to
 * [com.tekmoon.data.networking.HttpClientFactory]. The HTTP layer calls [getToken] before
 * every authenticated request and [refreshToken] exactly once when the server returns 401
 * (subsequent concurrent callers wait for that single refresh to finish, then reuse the result).
 *
 * Example implementation sketch:
 * ```kotlin
 * class MyTokenProvider(
 *     private val tokenStorage: TokenStorage,      // e.g. EncryptedSharedPrefs / Keychain
 *     private val authApi: AuthApi,
 * ) : TokenProvider {
 *
 *     override suspend fun getToken(): String? = tokenStorage.accessToken
 *
 *     override suspend fun refreshToken(): Result<String, DataError> {
 *         val refreshToken = tokenStorage.refreshToken
 *             ?: return Result.Failure(DataError.ClientError.Unauthorized)
 *         return authApi.refresh(refreshToken).onSuccess { newToken ->
 *             tokenStorage.accessToken = newToken
 *         }
 *     }
 * }
 * ```
 */
interface TokenProvider {

    /**
     * Returns the current access token, or `null` if the user is not authenticated.
     *
     * Called before every request that does not carry [NoAuthKey]. The returned value is
     * injected as `Authorization: Bearer <token>`. A `null` return means no header is injected
     * and the request goes out unauthenticated — if the server then returns 401 the interceptor
     * will not attempt a refresh (there is nothing to refresh).
     */
    suspend fun getToken(): String?

    /**
     * Called once when the server returns HTTP 401 on an authenticated request.
     *
     * The HTTP interceptor holds a [kotlinx.coroutines.sync.Mutex] around this call, so only
     * **one** coroutine executes [refreshToken] at a time even if many requests received 401
     * concurrently. All waiting callers reuse the token obtained by whichever coroutine won the
     * mutex — the refresh endpoint is called at most once per expiry cycle.
     *
     * @return [Result.Success] with the new access token on success, or [Result.Failure] with a
     *   [DataError] on failure (e.g. the refresh token is expired or the server is unreachable).
     *   A failure causes [com.tekmoon.data.networking.HttpClientFactory.authEvents] to emit
     *   [AuthEvent.SessionExpired].
     */
    suspend fun refreshToken(): Result<String, DataError>
}
