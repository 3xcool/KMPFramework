package com.tekmoon.data.networking

import io.ktor.client.engine.HttpClientEngine

/**
 * Returns the default [HttpClientEngine] for the current platform. Used by
 * `Framework.start(...)` to wire up the default `HttpClient` so consumer apps don't have to know
 * which engine to plug in per target.
 *
 * - Android / JVM: OkHttp
 * - iOS / macOS: Darwin (NSURLSession-based)
 */
expect fun platformHttpEngine(): HttpClientEngine
