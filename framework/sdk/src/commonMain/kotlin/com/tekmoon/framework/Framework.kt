package com.tekmoon.framework

import com.tekmoon.data.FrameworkConfig
import com.tekmoon.data.logging.SimpleLogger
import com.tekmoon.data.networking.HttpClientFactory
import com.tekmoon.data.networking.platformHttpEngine
import com.tekmoon.logger.Loggers
import com.tekmoon.logger.ShowMeLoggerK
import com.tekmoon.logger.domain.DefaultLoggerConfig
import com.tekmoon.logger.domain.LoggerConfig
import com.tekmoon.utilities.DispatcherProvider
import com.tekmoon.utilities.StandardDispatchers
import io.ktor.client.HttpClient
import kotlin.concurrent.Volatile

/**
 * Tekmoon KMP Framework umbrella module.
 *
 * Single entry point that bootstraps the framework: configuration, logger, HTTP client,
 * dispatchers. Idempotent — calling [start] twice returns the same [FrameworkState].
 *
 * Usage from a consumer app (commonMain), e.g. inside the root composable:
 * ```
 * remember {
 *     Framework.start(
 *         FrameworkInit(
 *             apiBaseUrl = AppBuildKonfig.API_BASE_URL,
 *             apiKey = AppBuildKonfig.API_KEY,
 *             environment = AppBuildKonfig.ENVIRONMENT,
 *             debug = AppBuildKonfig.DEBUG,
 *             isPro = AppBuildKonfig.IS_PRO,
 *             adEnabled = AppBuildKonfig.AD_ENABLED,
 *         )
 *     )
 * }
 * ```
 *
 * Then anywhere in the app:
 * ```
 * val logger = Framework.logger
 * val client = Framework.httpClient
 * val dispatchers = Framework.dispatchers
 * ```
 */
object Framework {

    const val VERSION = "0.0.5"

    @Volatile
    private var _state: FrameworkState? = null

    /** Currently installed [FrameworkState]; throws if [start] was not called yet. */
    val state: FrameworkState
        get() = _state
            ?: error("Framework.start(...) must be called before accessing Framework.state")

    /** True once [start] has been called. Mostly useful in tests / sample apps. */
    val isStarted: Boolean
        get() = _state != null

    // ----- Convenience accessors so consumers don't have to type Framework.state.xxx -----

    val logger: ShowMeLoggerK
        get() = state.logger

    val httpClient: HttpClient
        get() = state.httpClient

    val dispatchers: DispatcherProvider
        get() = state.dispatchers

    val config: FrameworkConfig.Snapshot
        get() = state.config

    /**
     * Initializes every framework subsystem and returns a [FrameworkState] handle. Safe to call
     * multiple times — subsequent calls return the already-initialized state without re-running
     * the bootstrap (useful when `start()` lives inside a Compose `remember {}`).
     */
    fun start(init: FrameworkInit): FrameworkState {
        _state?.let { return it }

        // 1. Runtime config
        FrameworkConfig.init(
            apiBaseUrl = init.apiBaseUrl,
            apiKey = init.apiKey,
            environment = init.environment,
            debug = init.debug,
            isPro = init.isPro,
            adEnabled = init.adEnabled,
        )

        // 2. Logger (Default in prod, swappable via FrameworkInit.loggerConfig)
        val loggerConfig = init.loggerConfig ?: DefaultLoggerConfig(enabled = true)
        val logger = ShowMeLoggerK(loggerConfig).also { it.initConfig() }
        Loggers.install(logger)

        // 3. HTTP client wired to the framework logger
        val httpClient = HttpClientFactory(ShowMeLoggerSimpleLoggerAdapter(logger))
            .create(platformHttpEngine())

        // 4. Dispatchers (default to StandardDispatchers; tests can inject)
        val dispatchers = init.dispatchers ?: StandardDispatchers

        val newState = FrameworkState(
            config = FrameworkConfig.snapshot(),
            logger = logger,
            httpClient = httpClient,
            dispatchers = dispatchers,
        )
        _state = newState
        return newState
    }

    /** Resets the installed state. **Tests only** — production code should never call this. */
    fun reset() {
        _state?.httpClient?.close()
        _state = null
        Loggers.reset()
    }
}

/** Input bag for [Framework.start]. */
data class FrameworkInit(
    val apiBaseUrl: String,
    val apiKey: String = "",
    val environment: String = "dev",
    val debug: Boolean = false,
    val isPro: Boolean = false,
    val adEnabled: Boolean = !isPro,
    /** Override the default [DispatcherProvider] (tests inject `TestDispatchers` here). */
    val dispatchers: DispatcherProvider? = null,
    /** Override the default [LoggerConfig] to plug in custom writers (file, remote, breadcrumbs). */
    val loggerConfig: LoggerConfig? = null,
)

/** Immutable handle to the initialized framework. */
data class FrameworkState internal constructor(
    val config: FrameworkConfig.Snapshot,
    val logger: ShowMeLoggerK,
    val httpClient: HttpClient,
    val dispatchers: DispatcherProvider,
)

/**
 * Bridges [ShowMeLoggerK] to the [SimpleLogger] interface that [HttpClientFactory] expects,
 * so the Ktor `Logging` plugin output ends up in the same writers as the rest of the app.
 */
private class ShowMeLoggerSimpleLoggerAdapter(private val logger: ShowMeLoggerK) : SimpleLogger {
    override fun debug(message: String) = logger.d(message)
    override fun info(message: String) = logger.i(message)
    override fun warn(message: String) = logger.w(message)
    override fun error(message: String, throwable: Throwable?) {
        if (throwable == null) logger.e(message) else logger.e("$message -> ${throwable.stackTraceToString()}")
    }
}
