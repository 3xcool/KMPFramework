package com.tekmoon.backgroundwork

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Maps a task [BackgroundTask.kind] to the [BackgroundTaskHandler] that
 * runs it.
 *
 * Apps register every kind they'll schedule at startup (typically from
 * `Framework.start`). The scheduler queries the registry when a task
 * fires.
 *
 * Thread-safe: backed by a [Mutex] so multiple modules can register kinds
 * concurrently without racing.
 */
public class BackgroundTaskRegistry {
    private val mutex = Mutex()
    private val handlers: MutableMap<String, BackgroundTaskHandler> = mutableMapOf()

    public suspend fun register(kind: String, handler: BackgroundTaskHandler) {
        mutex.withLock { handlers[kind] = handler }
    }

    public suspend fun unregister(kind: String) {
        mutex.withLock { handlers.remove(kind) }
    }

    public fun handler(kind: String): BackgroundTaskHandler? = handlers[kind]

    public fun kinds(): Set<String> = handlers.keys.toSet()
}
