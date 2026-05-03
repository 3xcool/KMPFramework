package com.tekmoon.utilities

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Abstraction over [CoroutineDispatcher] selection so callers can inject test dispatchers
 * (e.g., StandardTestDispatcher) without depending on [Dispatchers] directly.
 *
 * Use [StandardDispatchers] for production code, swap with a test-double in unit tests.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val mainImmediate: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/**
 * Default [DispatcherProvider] backed by [Dispatchers].
 */
object StandardDispatchers : DispatcherProvider {
    override val main: CoroutineDispatcher
        get() = Dispatchers.Main
    override val mainImmediate: CoroutineDispatcher
        get() = Dispatchers.Main.immediate
    override val io: CoroutineDispatcher
        get() = Dispatchers.IO
    override val default: CoroutineDispatcher
        get() = Dispatchers.Default
}
