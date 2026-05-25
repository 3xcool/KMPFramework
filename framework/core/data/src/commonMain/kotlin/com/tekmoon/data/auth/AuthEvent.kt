package com.tekmoon.data.auth

/**
 * Events emitted by the HTTP auth interceptor on the
 * [com.tekmoon.data.networking.HttpClientFactory.authEvents] shared flow.
 *
 * Collect this flow in your navigation layer (e.g. in the root ViewModel or an app-level
 * coroutine) and react accordingly:
 *
 * ```kotlin
 * httpClientFactory.authEvents.collect { event ->
 *     when (event) {
 *         AuthEvent.SessionExpired -> navController.navigate(LoginDestination)
 *     }
 * }
 * ```
 */
sealed interface AuthEvent {

    /**
     * Emitted when [TokenProvider.refreshToken] fails — meaning the refresh token is expired,
     * revoked, or the network is down during the refresh attempt.
     *
     * The failed request is NOT retried. Its caller receives the original [DataError] from
     * [com.tekmoon.data.networking.platformSafeCall]. The app is responsible for clearing local
     * session state and navigating to the login screen.
     */
    data object SessionExpired : AuthEvent
}
