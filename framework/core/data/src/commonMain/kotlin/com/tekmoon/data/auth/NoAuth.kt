package com.tekmoon.data.auth

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

/**
 * Ktor attribute that marks a request as not requiring an `Authorization` header.
 *
 * The auth interceptor checks for this key before injecting the Bearer token and before
 * attempting a refresh on a 401 response. Any request carrying this attribute is passed
 * through untouched.
 *
 * @see noAuth
 */
val NoAuthKey: AttributeKey<Unit> = AttributeKey("NoAuth")

/**
 * Marks this request as not requiring an `Authorization: Bearer` header.
 *
 * Use this for public endpoints (login, register, password reset, etc.) that must not
 * carry credentials:
 *
 * ```kotlin
 * // Login — no token needed
 * client.post("/auth/login") {
 *     noAuth()
 *     setBody(LoginRequest(email, password))
 * }
 *
 * // Public feed — no token needed
 * client.get("/feed/trending") {
 *     noAuth()
 * }
 *
 * // Profile — token injected automatically (no noAuth() call)
 * client.get("/users/me")
 * ```
 */
fun HttpRequestBuilder.noAuth() {
    attributes.put(NoAuthKey, Unit)
}
