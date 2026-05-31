package com.tekmoon.logger

import kotlin.concurrent.Volatile

/**
 * Process-wide [ShowMeLoggerK] handle, populated by `Framework.start(...)`.
 *
 * Modules that want a logger by default (e.g. [com.tekmoon.presentation.viewmodel.CommonViewModel])
 * read [current] so they don't have to take a logger parameter in every constructor. Returns `null`
 * if `Framework.start()` has not been called yet; callers should fall back gracefully.
 *
 * The setter ([install]) is `internal`-equivalent (just a regular function in this package) — there
 * is no contract that prevents an app from calling it directly, but the intended caller is
 * `Framework.start()` in `framework:sdk`.
 */
object Loggers {

    @Volatile
    private var _current: ShowMeLoggerK? = null

    /** Currently installed logger, or `null` if `Framework.start(...)` was not called. */
    val current: ShowMeLoggerK?
        get() = _current

    /**
     * Installs [logger] as the process-wide default. Typically called once by `Framework.start()`.
     * Subsequent calls overwrite the previous value (useful in tests that need to swap loggers).
     */
    fun install(logger: ShowMeLoggerK) {
        _current = logger
    }

    /** Clears the installed logger. Mainly useful in tests. */
    fun reset() {
        _current = null
    }
}
