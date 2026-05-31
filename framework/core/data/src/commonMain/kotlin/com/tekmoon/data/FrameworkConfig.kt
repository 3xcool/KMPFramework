package com.tekmoon.data

/**
 * Runtime configuration for the Tekmoon framework.
 *
 * In new code, prefer calling `Framework.start(FrameworkInit(...))` from `framework:sdk` instead
 * of [init] directly — `Framework.start(...)` initializes the logger, HTTP client, and any other
 * subsystems in one call (and itself calls [init] under the hood).
 *
 * Example (legacy):
 * ```
 * FrameworkConfig.init(
 *     apiBaseUrl = "https://api.myapp.com",
 *     apiKey = "my-api-key",
 *     environment = "prod",
 *     debug = false,
 *     isPro = true,
 *     adEnabled = false,
 * )
 * ```
 */
object FrameworkConfig {

    var apiBaseUrl: String = ""
        private set

    var apiKey: String = ""
        private set

    var environment: String = "dev"
        private set

    var debug: Boolean = true
        private set

    var isPro: Boolean = false
        private set

    var adEnabled: Boolean = true
        private set

    private var initialized = false

    /** True after [init] has been called at least once. */
    val isInitialized: Boolean
        get() = initialized

    fun init(
        apiBaseUrl: String,
        apiKey: String = "",
        environment: String = "dev",
        debug: Boolean = false,
        isPro: Boolean = false,
        adEnabled: Boolean = !isPro,
    ) {
        this.apiBaseUrl = apiBaseUrl
        this.apiKey = apiKey
        this.environment = environment
        this.debug = debug
        this.isPro = isPro
        this.adEnabled = adEnabled
        this.initialized = true
    }

    /**
     * Returns an immutable view of the current configuration. Use this when a consumer wants to
     * read the active config without depending on the singleton's mutability (e.g. inside
     * `FrameworkState`).
     */
    fun snapshot(): Snapshot = Snapshot(
        apiBaseUrl = apiBaseUrl,
        apiKey = apiKey,
        environment = environment,
        debug = debug,
        isPro = isPro,
        adEnabled = adEnabled,
    )

    /** Immutable point-in-time copy of [FrameworkConfig]. */
    data class Snapshot(
        val apiBaseUrl: String,
        val apiKey: String,
        val environment: String,
        val debug: Boolean,
        val isPro: Boolean,
        val adEnabled: Boolean,
    )
}
