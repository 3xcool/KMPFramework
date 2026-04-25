package com.tekmoon.data

/**
 * Runtime configuration for the Tekmoon framework.
 *
 * Consumer apps must call [init] before using framework networking features.
 *
 * Example:
 * ```
 * FrameworkConfig.init(
 *     apiBaseUrl = "https://api.myapp.com",
 *     apiKey = "my-api-key",
 *     environment = "prod",
 *     debug = false,
 *     isPro = true,
 *     adEnabled = false
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

    fun init(
        apiBaseUrl: String,
        apiKey: String = "",
        environment: String = "dev",
        debug: Boolean = false,
        isPro: Boolean = false,
        adEnabled: Boolean = !isPro
    ) {
        this.apiBaseUrl = apiBaseUrl
        this.apiKey = apiKey
        this.environment = environment
        this.debug = debug
        this.isPro = isPro
        this.adEnabled = adEnabled
        this.initialized = true
    }
}
