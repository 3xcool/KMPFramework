package com.tekmoon.data.realtime

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * Describes how the auth token is attached to the WebSocket upgrade request.
 *
 * Only used when [RealtimeConfig.tokenProvider] is non-null and returns a token.
 */
sealed interface TokenDelivery {

    /**
     * Appends the token as a URL query parameter.
     *
     * Example URL: `wss://api.example.com/ws?token=<token>`
     *
     * @param name The query parameter name. Defaults to `"token"`.
     */
    data class QueryParam(val name: String = "token") : TokenDelivery

    /**
     * Injects the token into an HTTP request header.
     *
     * Example header: `Authorization: Bearer <token>`
     *
     * @param name   Header name. Defaults to [HttpHeaders.Authorization].
     * @param prefix Value prefix (e.g. `"Bearer"`). Set to `""` to send the raw token.
     */
    data class Header(
        val name: String = HttpHeaders.Authorization,
        val prefix: String = "Bearer",
    ) : TokenDelivery

    /**
     * Fully custom delivery — receives the token and the Ktor [HttpRequestBuilder]
     * so the caller can do anything (multiple headers, custom signing, etc.).
     */
    data class Custom(
        val apply: suspend (token: String, request: HttpRequestBuilder) -> Unit,
    ) : TokenDelivery
}

/** Applies this [TokenDelivery] strategy to a Ktor [HttpRequestBuilder]. */
internal suspend fun TokenDelivery.applyToken(token: String, request: HttpRequestBuilder) {
    when (this) {
        is TokenDelivery.Header -> {
            val value = if (prefix.isNotEmpty()) "$prefix $token" else token
            request.header(name, value)
        }
        is TokenDelivery.QueryParam -> {
            request.url.parameters.append(name, token)
        }
        is TokenDelivery.Custom -> apply(token, request)
    }
}
